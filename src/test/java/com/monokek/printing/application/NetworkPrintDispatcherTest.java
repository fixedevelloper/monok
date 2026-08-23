package com.monokek.printing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monokek.printing.application.dto.KitchenTicketContent;
import com.monokek.printing.application.dto.ReceiptContent;
import com.monokek.printing.application.dto.SessionSummaryContent;
import com.monokek.printing.domain.Printer;
import com.monokek.printing.domain.PrintQueue;
import com.monokek.printing.domain.PrintQueueRepository;
import com.monokek.printing.infrastructure.LogoImageLoader;
import com.monokek.printing.infrastructure.NetworkPrinterProbe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Same fake-{@link ServerSocket}-printer approach as {@code PrinterServiceTest},
 * but for the real order/receipt/session-report rendering path: hand-built
 * content goes in, real ESC/POS bytes (product names, prices, sections, the
 * cut sequence) must come out on the wire.
 */
class NetworkPrintDispatcherTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @Timeout(10)
    void dispatchSendsARealReceiptWithRoundsAndMarksTheJobCompleted() throws Exception {
        try (ServerSocket fakePrinter = new ServerSocket(0)) {
            CompletableFuture<byte[]> received = acceptProbeThenCaptureBytes(fakePrinter);

            Printer printer = networkPrinter(fakePrinter.getLocalPort());
            ReceiptContent content = new ReceiptContent(
                    10L, "uuid-1", "CMD-0001", "Mono Kek", "Douala", "699000000", null, "Table 3", "Awa", "Paul",
                    List.of(new ReceiptContent.RoundSection(1,
                            List.of(new ReceiptContent.TicketItem("Soya Boeuf", 2, new BigDecimal("1000"), new BigDecimal("2000"), List.of("Piment"))))),
                    new BigDecimal("2000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2000"),
                    "cash", new BigDecimal("2000"), BigDecimal.ZERO);

            PrintQueueRepository printQueueRepository = mock(PrintQueueRepository.class);
            AtomicReference<PrintQueue> saved = new AtomicReference<>();
            when(printQueueRepository.findById(5L)).thenAnswer(inv -> Optional.ofNullable(saved.get()));
            when(printQueueRepository.save(any(PrintQueue.class))).thenAnswer(inv -> {
                PrintQueue job = inv.getArgument(0);
                saved.set(job);
                return job;
            });
            PrintQueue existing = new PrintQueue();
            existing.setId(5L);
            existing.setStatus("pending");
            saved.set(existing);

            NetworkPrintDispatcher dispatcher = new NetworkPrintDispatcher(
                    new NetworkPrinterProbe(), renderer(), new PrintQueueService(printQueueRepository), objectMapper);

            dispatcher.dispatch(printer, 5L, "receipt", objectMapper.writeValueAsString(content));

            byte[] bytes = received.get(5, TimeUnit.SECONDS);
            String ascii = new String(bytes, StandardCharsets.US_ASCII);

            assertThat(ascii).contains("Soya Boeuf").contains("Piment").contains("Mono Kek")
                    .contains("CMD-0001").contains("SERVICE #1").contains("Awa").contains("REGLEMENTS");
            assertThat(bytes).containsSequence((byte) 0x1D, (byte) 0x56, (byte) 0x30); // GS V 48 = full cut

            assertThat(saved.get().getStatus()).isEqualTo("completed");
            assertThat(saved.get().getAttempts()).isEqualTo(1);
        }
    }

    @Test
    @Timeout(10)
    void dispatchSendsARealReceiptEvenWhenTheLogoUrlIsUnreachable() throws Exception {
        try (ServerSocket fakePrinter = new ServerSocket(0)) {
            CompletableFuture<byte[]> received = acceptProbeThenCaptureBytes(fakePrinter);

            Printer printer = networkPrinter(fakePrinter.getLocalPort());
            ReceiptContent content = new ReceiptContent(
                    11L, "uuid-2", "CMD-0002", "Mono Kek", null, null, "http://127.0.0.1:1/logo.png", "Table 1", null, null,
                    List.of(new ReceiptContent.RoundSection(1,
                            List.of(new ReceiptContent.TicketItem("Coca", 1, new BigDecimal("500"), new BigDecimal("500"), List.of())))),
                    new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("500"),
                    "cash", new BigDecimal("500"), BigDecimal.ZERO);

            PrintQueueRepository printQueueRepository = mock(PrintQueueRepository.class);
            PrintQueue existing = new PrintQueue();
            existing.setId(6L);
            when(printQueueRepository.findById(6L)).thenReturn(Optional.of(existing));
            when(printQueueRepository.save(any(PrintQueue.class))).thenAnswer(inv -> inv.getArgument(0));

            NetworkPrintDispatcher dispatcher = new NetworkPrintDispatcher(
                    new NetworkPrinterProbe(), renderer(), new PrintQueueService(printQueueRepository), objectMapper);

            dispatcher.dispatch(printer, 6L, "receipt", objectMapper.writeValueAsString(content));

            byte[] bytes = received.get(5, TimeUnit.SECONDS);
            assertThat(new String(bytes, StandardCharsets.US_ASCII)).contains("Coca").contains("CMD-0002");
            assertThat(existing.getStatus()).isEqualTo("completed");
        }
    }

    @Test
    @Timeout(10)
    void dispatchSendsARealKitchenTicket() throws Exception {
        try (ServerSocket fakePrinter = new ServerSocket(0)) {
            CompletableFuture<byte[]> received = acceptProbeThenCaptureBytes(fakePrinter);

            Printer printer = networkPrinter(fakePrinter.getLocalPort());
            printer.setLocation("kitchen");
            KitchenTicketContent content = new KitchenTicketContent(
                    1L, 2L, "Table 7", "Awa", "Sans oignons",
                    List.of(new KitchenTicketContent.TicketItem("Poulet DG", 1, List.of("Extra piment"))));

            PrintQueueRepository printQueueRepository = mock(PrintQueueRepository.class);
            PrintQueue existing = new PrintQueue();
            existing.setId(9L);
            when(printQueueRepository.findById(9L)).thenReturn(Optional.of(existing));
            when(printQueueRepository.save(any(PrintQueue.class))).thenAnswer(inv -> inv.getArgument(0));

            NetworkPrintDispatcher dispatcher = new NetworkPrintDispatcher(
                    new NetworkPrinterProbe(), renderer(), new PrintQueueService(printQueueRepository), objectMapper);

            dispatcher.dispatch(printer, 9L, "kitchen", objectMapper.writeValueAsString(content));

            byte[] bytes = received.get(5, TimeUnit.SECONDS);
            String ascii = new String(bytes, StandardCharsets.US_ASCII);

            assertThat(ascii).contains("BON KITCHEN").contains("POULET DG").contains("Extra piment").contains("Awa").contains("Sans oignons");
        }
    }

    @Test
    @Timeout(10)
    void dispatchSendsARealSessionSummary() throws Exception {
        try (ServerSocket fakePrinter = new ServerSocket(0)) {
            CompletableFuture<byte[]> received = acceptProbeThenCaptureBytes(fakePrinter);

            Printer printer = networkPrinter(fakePrinter.getLocalPort());
            SessionSummaryContent content = new SessionSummaryContent(
                    42L, "Mono Kek", "Awa", LocalDateTime.now().minusHours(8), LocalDateTime.now(),
                    new BigDecimal("10000"), new BigDecimal("50000"), new BigDecimal("60000"), new BigDecimal("59500"), new BigDecimal("-500"),
                    List.of(new SessionSummaryContent.PaymentBreakdown("cash", new BigDecimal("50000"))),
                    List.of(new SessionSummaryContent.SoldItem("Soya Boeuf", 5, new BigDecimal("5000"))));

            PrintQueueRepository printQueueRepository = mock(PrintQueueRepository.class);
            PrintQueue existing = new PrintQueue();
            existing.setId(13L);
            when(printQueueRepository.findById(13L)).thenReturn(Optional.of(existing));
            when(printQueueRepository.save(any(PrintQueue.class))).thenAnswer(inv -> inv.getArgument(0));

            NetworkPrintDispatcher dispatcher = new NetworkPrintDispatcher(
                    new NetworkPrinterProbe(), renderer(), new PrintQueueService(printQueueRepository), objectMapper);

            dispatcher.dispatch(printer, 13L, "session_summary", objectMapper.writeValueAsString(content));

            byte[] bytes = received.get(5, TimeUnit.SECONDS);
            String ascii = new String(bytes, StandardCharsets.US_ASCII);

            assertThat(ascii).contains("RECAPITULATIF DE CAISSE").contains("Awa").contains("MODES DE REGLEMENTS")
                    .contains("ARTICLES VENDUS").contains("Soya Boeuf");
            assertThat(existing.getStatus()).isEqualTo("completed");
        }
    }

    @Test
    void dispatchMarksTheJobFailedWhenThePrinterIsUnreachable() {
        Printer printer = networkPrinter(1); // reserved port, nothing listens there

        PrintQueueRepository printQueueRepository = mock(PrintQueueRepository.class);
        PrintQueue existing = new PrintQueue();
        existing.setId(7L);
        when(printQueueRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(printQueueRepository.save(any(PrintQueue.class))).thenAnswer(inv -> inv.getArgument(0));

        NetworkPrintDispatcher dispatcher = new NetworkPrintDispatcher(
                new NetworkPrinterProbe(), renderer(), new PrintQueueService(printQueueRepository), objectMapper);

        dispatcher.dispatch(printer, 7L, "receipt", "{}");

        assertThat(existing.getStatus()).isEqualTo("failed");
        assertThat(existing.getErrorMessage()).isNotBlank();
    }

    private EscPosTicketRenderer renderer() {
        return new EscPosTicketRenderer(new LogoImageLoader(), objectMapper);
    }

    private CompletableFuture<byte[]> acceptProbeThenCaptureBytes(ServerSocket fakePrinter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                try (Socket probeConnection = fakePrinter.accept()) {
                    // NetworkPrinterProbe: connects and disconnects without sending anything.
                }
                try (Socket dataConnection = fakePrinter.accept()) {
                    return dataConnection.getInputStream().readAllBytes();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Printer networkPrinter(int port) {
        Printer printer = new Printer();
        printer.setName("Cuisine");
        printer.setConnection("network");
        printer.setIp("127.0.0.1");
        printer.setPort(port);
        return printer;
    }
}
