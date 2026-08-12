package com.monokek.printing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.TcpIpOutputStream;
import com.monokek.printing.application.dto.KitchenTicketContent;
import com.monokek.printing.application.dto.ReceiptContent;
import com.monokek.printing.application.dto.SessionSummaryContent;
import com.monokek.printing.domain.Printer;
import com.monokek.printing.infrastructure.NetworkPrinterProbe;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Renders and sends a just-queued job to a network printer right away —
 * triggered synchronously from {@link PrintQueueListener}, which already runs
 * on Spring Modulith's async event-listener thread, so this never blocks the
 * HTTP request that placed the order/payment. USB printers are untouched:
 * they stay {@code pending} for whatever LAN worker polls {@code GET
 * /api/print-queue/pending}, exactly as before.
 */
@Component
public class NetworkPrintDispatcher {

    private final NetworkPrinterProbe networkPrinterProbe;
    private final EscPosTicketRenderer renderer;
    private final PrintQueueService printQueueService;
    private final ObjectMapper objectMapper;

    public NetworkPrintDispatcher(
            NetworkPrinterProbe networkPrinterProbe, EscPosTicketRenderer renderer,
            PrintQueueService printQueueService, ObjectMapper objectMapper) {
        this.networkPrinterProbe = networkPrinterProbe;
        this.renderer = renderer;
        this.printQueueService = printQueueService;
        this.objectMapper = objectMapper;
    }

    public void dispatch(Printer printer, Long jobId, String jobType, String contentJson) {
        if (!networkPrinterProbe.isReachable(printer.getIp(), printer.getPort())) {
            printQueueService.markFailed(jobId, "Imprimante injoignable");
            return;
        }

        try (TcpIpOutputStream out = new TcpIpOutputStream(printer.getIp(), printer.getPort())) {
            EscPos escpos = new EscPos(out);
            switch (jobType) {
                case "receipt" -> renderer.renderReceipt(escpos, objectMapper.readValue(contentJson, ReceiptContent.class), printer);
                case "session_summary" -> renderer.renderSessionSummary(escpos, objectMapper.readValue(contentJson, SessionSummaryContent.class), printer);
                default -> renderer.renderKitchen(escpos, objectMapper.readValue(contentJson, KitchenTicketContent.class), printer);
            }
            escpos.flush();
            printQueueService.markSuccess(jobId);
        } catch (IOException e) {
            printQueueService.markFailed(jobId, "Échec d'impression : " + e.getMessage());
        }
    }
}
