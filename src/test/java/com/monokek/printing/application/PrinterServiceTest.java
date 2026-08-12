package com.monokek.printing.application;

import com.monokek.printing.domain.Printer;
import com.monokek.printing.domain.PrinterRepository;
import com.monokek.printing.web.dto.TestConnectionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Stands in for a real network printer with a plain {@link ServerSocket} on
 * localhost, so {@code testConnection} can be verified end-to-end (real
 * escpos-coffee bytes over a real TCP socket) without any physical hardware.
 */
class PrinterServiceTest {

    @Test
    @Timeout(10)
    void testConnectionSendsRealEscPosBytesToTheNetworkPrinter() throws Exception {
        try (ServerSocket fakePrinter = new ServerSocket(0)) {
            // testConnection opens two separate TCP connections: a plain reachability
            // probe first (see the method's Javadoc), then the real ESC/POS stream.
            CompletableFuture<byte[]> received = CompletableFuture.supplyAsync(() -> {
                try {
                    try (Socket probeConnection = fakePrinter.accept()) {
                        // Reachability probe: connects and disconnects without sending anything.
                    }
                    try (Socket dataConnection = fakePrinter.accept()) {
                        return dataConnection.getInputStream().readAllBytes();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Printer printer = new Printer();
            printer.setId(1L);
            printer.setName("Cuisine");
            printer.setConnection("network");
            printer.setIp("127.0.0.1");
            printer.setPort(fakePrinter.getLocalPort());

            PrinterRepository repository = mock(PrinterRepository.class);
            when(repository.findById(1L)).thenReturn(Optional.of(printer));

            TestConnectionResult result = new PrinterService(repository, new com.monokek.printing.infrastructure.NetworkPrinterProbe()).testConnection(1L);

            assertThat(result.status()).isEqualTo("online");

            byte[] bytes = received.get(5, TimeUnit.SECONDS);
            String ascii = new String(bytes, StandardCharsets.US_ASCII);

            // Ticket text made it onto the wire.
            assertThat(ascii).contains("TEST D'IMPRESSION").contains("Cuisine");
            // GS V 48 = ESC/POS "full cut" command.
            assertThat(bytes).containsSequence((byte) 0x1D, (byte) 0x56, (byte) 0x30);
        }
    }

    @Test
    void testConnectionReportsOfflineWhenNothingIsListening() {
        Printer printer = new Printer();
        printer.setId(2L);
        printer.setName("Bar");
        printer.setConnection("network");
        printer.setIp("127.0.0.1");
        printer.setPort(1); // reserved port, nothing listens there

        PrinterRepository repository = mock(PrinterRepository.class);
        when(repository.findById(2L)).thenReturn(Optional.of(printer));

        TestConnectionResult result = new PrinterService(repository, new com.monokek.printing.infrastructure.NetworkPrinterProbe()).testConnection(2L);

        assertThat(result.status()).isEqualTo("offline");
    }
}
