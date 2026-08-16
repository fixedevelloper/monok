package com.monokek.printing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monokek.ordering.domain.event.KitchenTicketRequestedEvent;
import com.monokek.printing.domain.PrintQueue;
import com.monokek.printing.domain.PrintQueueRepository;
import com.monokek.printing.domain.Printer;
import com.monokek.printing.domain.PrinterRepository;
import com.monokek.printing.domain.event.PrintJobQueuedEvent;
import com.monokek.settings.StoreSettings;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrintQueueListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void publishesAPrintJobQueuedEventForAUsbPrinterInsteadOfDispatchingOverTheNetwork() {
        Printer printer = usbPrinter(7L, 3L, "Cuisine", "cups-kitchen-1");

        PrinterRepository printerRepository = mock(PrinterRepository.class);
        when(printerRepository.findFirstByBranchIdAndLocationAndActiveTrue(3L, "kitchen")).thenReturn(Optional.of(printer));

        PrintQueueRepository printQueueRepository = mock(PrintQueueRepository.class);
        when(printQueueRepository.save(any(PrintQueue.class))).thenAnswer(inv -> {
            PrintQueue job = inv.getArgument(0);
            job.setId(42L);
            return job;
        });

        NetworkPrintDispatcher networkPrintDispatcher = mock(NetworkPrintDispatcher.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StoreSettings storeSettings = mock(StoreSettings.class);

        PrintQueueListener listener = new PrintQueueListener(
                printerRepository, printQueueRepository, objectMapper, storeSettings, networkPrintDispatcher, eventPublisher);

        KitchenTicketRequestedEvent event = new KitchenTicketRequestedEvent(
                1L, 2L, 3L, 9L, null, "Table 4", List.of(), "Awa");

        listener.on(event);

        verify(networkPrintDispatcher, never()).dispatch(any(), any(), any(), any());

        var captor = org.mockito.ArgumentCaptor.forClass(PrintJobQueuedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PrintJobQueuedEvent published = captor.getValue();
        assertThat(published.jobId()).isEqualTo(42L);
        assertThat(published.branchId()).isEqualTo(3L);
        assertThat(published.printerId()).isEqualTo(7L);
        assertThat(published.printerName()).isEqualTo("Cuisine");
        assertThat(published.osPrinterName()).isEqualTo("cups-kitchen-1");
        assertThat(published.jobType()).isEqualTo("kitchen");
    }

    @Test
    void doesNotPublishAnEventForANetworkPrinter() {
        Printer printer = new Printer();
        printer.setId(8L);
        printer.setBranchId(3L);
        printer.setConnection("network");
        printer.setIp("192.168.1.50");

        PrinterRepository printerRepository = mock(PrinterRepository.class);
        when(printerRepository.findFirstByBranchIdAndLocationAndActiveTrue(3L, "kitchen")).thenReturn(Optional.of(printer));

        PrintQueueRepository printQueueRepository = mock(PrintQueueRepository.class);
        when(printQueueRepository.save(any(PrintQueue.class))).thenAnswer(inv -> {
            PrintQueue job = inv.getArgument(0);
            job.setId(99L);
            return job;
        });

        NetworkPrintDispatcher networkPrintDispatcher = mock(NetworkPrintDispatcher.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StoreSettings storeSettings = mock(StoreSettings.class);

        PrintQueueListener listener = new PrintQueueListener(
                printerRepository, printQueueRepository, objectMapper, storeSettings, networkPrintDispatcher, eventPublisher);

        listener.on(new KitchenTicketRequestedEvent(1L, 2L, 3L, 9L, null, "Table 4", List.of(), "Awa"));

        verify(networkPrintDispatcher).dispatch(eq(printer), eq(99L), eq("kitchen"), any());
        verify(eventPublisher, never()).publishEvent(any(PrintJobQueuedEvent.class));
    }

    private Printer usbPrinter(Long id, Long branchId, String name, String osPrinterName) {
        Printer printer = new Printer();
        printer.setId(id);
        printer.setBranchId(branchId);
        printer.setName(name);
        printer.setConnection("usb");
        printer.setOsPrinterName(osPrinterName);
        return printer;
    }
}
