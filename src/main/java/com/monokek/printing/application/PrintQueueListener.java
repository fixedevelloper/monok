package com.monokek.printing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monokek.crm.domain.event.CouponPrintRequestedEvent;
import com.monokek.ordering.domain.event.KitchenTicketRequestedEvent;
import com.monokek.ordering.domain.event.OrderPaidEvent;
import com.monokek.ordering.domain.event.SessionReportReadyEvent;
import com.monokek.printing.application.dto.CouponContent;
import com.monokek.printing.application.dto.KitchenTicketContent;
import com.monokek.printing.application.dto.ReceiptContent;
import com.monokek.printing.application.dto.SessionSummaryContent;
import com.monokek.printing.domain.Printer;
import com.monokek.printing.domain.PrintQueue;
import com.monokek.printing.domain.PrintQueueRepository;
import com.monokek.printing.domain.PrinterRepository;
import com.monokek.printing.domain.event.PrintJobQueuedEvent;
import com.monokek.settings.StoreSettings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Reacts to {@code ordering}'s domain events instead of {@code ordering}
 * calling {@code PrintService::queueRoundTickets}/{@code queueFinalReceipt}
 * directly — replaces those two Laravel methods for both halves of printing:
 * deciding which printer + queuing a job (unchanged), and — for network
 * printers, via {@link NetworkPrintDispatcher} — actually rendering and
 * sending the ticket right away. USB jobs stay queued as before, for
 * whatever LAN worker polls {@code GET /api/print-queue/pending}.
 */
@Component
public class PrintQueueListener {

    private final PrinterRepository printerRepository;
    private final PrintQueueRepository printQueueRepository;
    private final ObjectMapper objectMapper;
    private final StoreSettings storeSettings;
    private final NetworkPrintDispatcher networkPrintDispatcher;
    private final ApplicationEventPublisher eventPublisher;

    public PrintQueueListener(
            PrinterRepository printerRepository, PrintQueueRepository printQueueRepository, ObjectMapper objectMapper,
            StoreSettings storeSettings, NetworkPrintDispatcher networkPrintDispatcher, ApplicationEventPublisher eventPublisher) {
        this.printerRepository = printerRepository;
        this.printQueueRepository = printQueueRepository;
        this.objectMapper = objectMapper;
        this.storeSettings = storeSettings;
        this.networkPrintDispatcher = networkPrintDispatcher;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    void on(KitchenTicketRequestedEvent event) {
        printerForLocation(event.branchId(), "kitchen").ifPresent(printer -> {
            List<KitchenTicketContent.TicketItem> items = event.items() == null ? List.of() : event.items().stream()
                    .map(i -> new KitchenTicketContent.TicketItem(i.productName(), i.qty(), i.modifierNames()))
                    .toList();
            KitchenTicketContent content = new KitchenTicketContent(
                    event.orderId(), event.roundId(), event.tableName(), event.serverName(), event.note(), items);
            dispatch(printer, "kitchen", content);
        });
    }

    @ApplicationModuleListener
    void on(OrderPaidEvent event) {
        printerForLocation(event.branchId(), "receipt").ifPresent(printer -> {
            StoreSettings.StoreInfo store = storeSettings.current();
            List<ReceiptContent.RoundSection> rounds = event.rounds() == null ? List.of() : event.rounds().stream()
                    .map(r -> new ReceiptContent.RoundSection(r.roundNumber(), r.items().stream()
                            .map(i -> new ReceiptContent.TicketItem(i.productName(), i.qty(), i.unitPrice(), i.total(), i.modifierNames()))
                            .toList()))
                    .toList();
            ReceiptContent content = new ReceiptContent(
                    event.orderId(), event.orderUuid().toString(), event.reference(),
                    store.name(), store.address(), store.phone(), store.logoUrl(),
                    event.tableName(), event.serverName(), rounds,
                    event.subtotal(), event.tax(), event.discount(), event.total(),
                    event.paymentMethod(), event.amountReceived(), event.changeDue());
            dispatch(printer, "receipt", content);
        });
    }

    @ApplicationModuleListener
    void on(SessionReportReadyEvent event) {
        printerForLocation(event.branchId(), "receipt").ifPresent(printer -> {
            StoreSettings.StoreInfo store = storeSettings.current();
            List<SessionSummaryContent.PaymentBreakdown> breakdown = event.paymentBreakdown() == null ? List.of() : event.paymentBreakdown().stream()
                    .map(b -> new SessionSummaryContent.PaymentBreakdown(b.method(), b.total()))
                    .toList();
            List<SessionSummaryContent.SoldItem> soldItems = event.soldItems() == null ? List.of() : event.soldItems().stream()
                    .map(i -> new SessionSummaryContent.SoldItem(i.productName(), i.qty(), i.total()))
                    .toList();
            SessionSummaryContent content = new SessionSummaryContent(
                    event.sessionId(), store.name(), event.cashierName(), event.openedAt(), event.closedAt(),
                    event.openingAmount(), event.totalSales(), event.expectedAmount(), event.actualAmount(), event.difference(),
                    breakdown, soldItems);
            dispatch(printer, "session_summary", content);
        });
    }

    @ApplicationModuleListener
    void on(CouponPrintRequestedEvent event) {
        printerForLocation(event.branchId(), "receipt").ifPresent(printer -> {
            StoreSettings.StoreInfo store = storeSettings.current();
            CouponContent content = new CouponContent(
                    store.name(), store.address(), store.phone(),
                    event.code(), event.amount(), event.expiresAt());
            dispatch(printer, "coupon", content);
        });
    }

    /** Falls back to the branch's receipt printer if there's no dedicated one for the location — port of {@code PrintService::queueRoundTickets}. */
    private Optional<Printer> printerForLocation(Long branchId, String location) {
        var dedicated = printerRepository.findFirstByBranchIdAndLocationAndActiveTrue(branchId, location);
        return dedicated.isPresent() ? dedicated : printerRepository.findFirstByBranchIdAndLocationAndActiveTrue(branchId, "receipt");
    }

    private void dispatch(Printer printer, String jobType, Object content) {
        PrintQueue job = queue(printer, jobType, content);
        if ("network".equals(printer.getConnection())) {
            networkPrintDispatcher.dispatch(printer, job.getId(), job.getJobType(), job.getContent());
        } else if ("usb".equals(printer.getConnection())) {
            eventPublisher.publishEvent(new PrintJobQueuedEvent(
                    job.getId(), printer.getBranchId(), printer.getId(), printer.getName(), printer.getOsPrinterName(),
                    job.getJobType(), job.getContent(), (short) job.getPriority()));
        }
    }

    private PrintQueue queue(Printer printer, String jobType, Object content) {
        PrintQueue job = new PrintQueue();
        job.setPrinter(printer);
        job.setJobType(jobType);
        job.setContent(toJson(content));
        job.setStatus("pending");
        return printQueueRepository.save(job);
    }

    private String toJson(Object content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser le contenu du job d'impression", e);
        }
    }
}
