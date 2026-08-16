package com.monokek.printing.domain.event;

import java.time.Instant;

/**
 * Published when a job is queued for a USB printer — the only kind
 * {@link com.monokek.printing.application.NetworkPrintDispatcher} doesn't
 * already handle synchronously. Consumed by {@code notifications.application.NotificationEventListener}
 * to push it in real time to whichever branch's Tauri POS app owns that
 * printer, over the existing SSE connection, instead of that app having to
 * poll for it.
 */
public record PrintJobQueuedEvent(
        Long jobId, Long branchId, Long printerId, String printerName, String osPrinterName,
        String jobType, String content, short priority, Instant occurredAt) {

    public PrintJobQueuedEvent(
            Long jobId, Long branchId, Long printerId, String printerName, String osPrinterName,
            String jobType, String content, short priority) {
        this(jobId, branchId, printerId, printerName, osPrinterName, jobType, content, priority, Instant.now());
    }
}
