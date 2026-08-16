package com.monokek.printing.application;

import com.monokek.printing.domain.PrintQueue;
import com.monokek.printing.domain.event.PrintJobQueuedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link NetworkPrintDispatcher} only ever attempts a job once, synchronously,
 * right when it's queued — a transient network blip (printer momentarily off,
 * Wi-Fi hiccup) then leaves the job stuck at {@code status = "failed"}
 * forever, even though {@code attempts} already tracks how many tries it's
 * had. This periodically re-dispatches those jobs, up to {@link #MAX_ATTEMPTS}
 * total tries.
 *
 * <p>USB jobs get the same treatment, but the server can't print to them
 * itself — it can only re-publish {@link PrintJobQueuedEvent} so
 * {@code notifications.NotificationEventListener} pushes the job again over
 * SSE to whichever Tauri app is (or becomes) connected for that branch. If
 * nothing is connected the push is just dropped and {@code attempts} stays
 * untouched — only a real attempt reported back by the client via
 * {@code mark-success}/{@code mark-failed} counts against {@link #MAX_ATTEMPTS},
 * so a USB job can wait indefinitely for its app to come back online instead
 * of being burned through by unheard redeliveries.
 */
@Service
public class PrintRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PrintRetryScheduler.class);

    static final int MAX_ATTEMPTS = 5;

    private final PrintQueueService printQueueService;
    private final NetworkPrintDispatcher networkPrintDispatcher;
    private final ApplicationEventPublisher eventPublisher;

    public PrintRetryScheduler(
            PrintQueueService printQueueService, NetworkPrintDispatcher networkPrintDispatcher, ApplicationEventPublisher eventPublisher) {
        this.printQueueService = printQueueService;
        this.networkPrintDispatcher = networkPrintDispatcher;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 60_000)
    public void retryFailedJobs() {
        List<PrintQueue> jobs = printQueueService.findRetryableJobs(MAX_ATTEMPTS);
        for (PrintQueue job : jobs) {
            try {
                if ("network".equals(job.getPrinter().getConnection())) {
                    networkPrintDispatcher.dispatch(job.getPrinter(), job.getId(), job.getJobType(), job.getContent());
                } else if ("usb".equals(job.getPrinter().getConnection())) {
                    eventPublisher.publishEvent(new PrintJobQueuedEvent(
                            job.getId(), job.getPrinter().getBranchId(), job.getPrinter().getId(), job.getPrinter().getName(),
                            job.getPrinter().getOsPrinterName(), job.getJobType(), job.getContent(), (short) job.getPriority()));
                }
            } catch (Exception e) {
                log.warn("Relance de l'impression {} échouée : {}", job.getId(), e.getMessage());
            }
        }
    }
}
