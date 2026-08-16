package com.monokek.printing.application;

import com.monokek.printing.domain.PrintQueue;
import com.monokek.printing.domain.Printer;
import com.monokek.printing.domain.event.PrintJobQueuedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrintRetrySchedulerTest {

    @Test
    void retriesFailedNetworkJobsByRedispatchingThemOverTcp() {
        PrintQueue networkJob = job(1L, "network", "kitchen", "{\"a\":1}");

        PrintQueueService printQueueService = mock(PrintQueueService.class);
        when(printQueueService.findRetryableJobs(PrintRetryScheduler.MAX_ATTEMPTS)).thenReturn(List.of(networkJob));

        NetworkPrintDispatcher dispatcher = mock(NetworkPrintDispatcher.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        new PrintRetryScheduler(printQueueService, dispatcher, eventPublisher).retryFailedJobs();

        verify(dispatcher, times(1)).dispatch(eq(networkJob.getPrinter()), eq(1L), eq("kitchen"), eq("{\"a\":1}"));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void retriesFailedUsbJobsByRepublishingThePrintJobQueuedEventInsteadOfDispatchingOverTcp() {
        PrintQueue usbJob = job(2L, "usb", "receipt", "{\"b\":2}");
        usbJob.getPrinter().setId(9L);
        usbJob.getPrinter().setBranchId(3L);
        usbJob.getPrinter().setName("Caisse");
        usbJob.getPrinter().setOsPrinterName("XPrinter");

        PrintQueueService printQueueService = mock(PrintQueueService.class);
        when(printQueueService.findRetryableJobs(PrintRetryScheduler.MAX_ATTEMPTS)).thenReturn(List.of(usbJob));

        NetworkPrintDispatcher dispatcher = mock(NetworkPrintDispatcher.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        new PrintRetryScheduler(printQueueService, dispatcher, eventPublisher).retryFailedJobs();

        verify(dispatcher, never()).dispatch(any(), any(), any(), any());

        var captor = org.mockito.ArgumentCaptor.forClass(PrintJobQueuedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PrintJobQueuedEvent published = captor.getValue();
        assertThat(published.jobId()).isEqualTo(2L);
        assertThat(published.branchId()).isEqualTo(3L);
        assertThat(published.printerId()).isEqualTo(9L);
        assertThat(published.osPrinterName()).isEqualTo("XPrinter");
        assertThat(published.jobType()).isEqualTo("receipt");
    }

    @Test
    void oneJobThrowingDoesNotStopTheRestOfTheBatchFromBeingRetried() {
        PrintQueue first = job(1L, "network", "kitchen", "{}");
        PrintQueue second = job(2L, "network", "receipt", "{}");

        PrintQueueService printQueueService = mock(PrintQueueService.class);
        when(printQueueService.findRetryableJobs(PrintRetryScheduler.MAX_ATTEMPTS)).thenReturn(List.of(first, second));

        NetworkPrintDispatcher dispatcher = mock(NetworkPrintDispatcher.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        doThrow(new RuntimeException("printer offline")).when(dispatcher).dispatch(any(), eq(1L), any(), any());

        new PrintRetryScheduler(printQueueService, dispatcher, eventPublisher).retryFailedJobs();

        verify(dispatcher).dispatch(eq(second.getPrinter()), eq(2L), eq("receipt"), eq("{}"));
    }

    private PrintQueue job(Long id, String connection, String jobType, String content) {
        Printer printer = new Printer();
        printer.setConnection(connection);
        PrintQueue job = new PrintQueue();
        job.setId(id);
        job.setPrinter(printer);
        job.setJobType(jobType);
        job.setContent(content);
        job.setStatus("failed");
        return job;
    }
}
