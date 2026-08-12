
package com.monokek.printing.application;

import com.monokek.common.ApiException;
import com.monokek.printing.domain.PrintQueue;
import com.monokek.printing.domain.PrintQueueRepository;
import com.monokek.printing.web.dto.PrintQueueDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service: port of the inline print-queue closures in
 * {@code routes/api.php}. Network-printer jobs are now rendered and sent by
 * {@link NetworkPrintDispatcher} right when they're queued, calling
 * {@link #markSuccess}/{@link #markFailed} itself. {@link #pending()} still
 * exists for USB jobs — an external LAN worker polls it, prints locally, and
 * reports back through the same two methods.
 */
@Service
public class PrintQueueService {

    private final PrintQueueRepository printQueueRepository;

    public PrintQueueService(PrintQueueRepository printQueueRepository) {
        this.printQueueRepository = printQueueRepository;
    }

    @Transactional(readOnly = true)
    public List<PrintQueueDto> pending() {
        return printQueueRepository.findByStatus("pending").stream().map(this::toDto).toList();
    }

    @Transactional
    public void markSuccess(Long jobId) {
        PrintQueue job = findOrThrow(jobId);
        job.setAttempts(job.getAttempts() + 1);
        job.setStatus("completed");
        printQueueRepository.save(job);
    }

    @Transactional
    public void markFailed(Long jobId) {
        markFailed(jobId, null);
    }

    /** Used by {@link NetworkPrintDispatcher} to record why a network print attempt failed. */
    @Transactional
    public void markFailed(Long jobId, String errorMessage) {
        PrintQueue job = findOrThrow(jobId);
        job.setAttempts(job.getAttempts() + 1);
        job.setStatus("failed");
        job.setErrorMessage(errorMessage);
        printQueueRepository.save(job);
    }

    private PrintQueue findOrThrow(Long jobId) {
        return printQueueRepository.findById(jobId).orElseThrow(() -> ApiException.notFound("Job d'impression introuvable"));
    }

    private PrintQueueDto toDto(PrintQueue job) {
        return new PrintQueueDto(
                job.getId(), job.getPrinter().getId(), job.getPrinter().getName(), job.getPrinter().getIp(), job.getPrinter().getPort(),
                job.getJobType(), job.getContent(), job.getAttempts(), job.getStatus(), job.getErrorMessage(), job.getPriority());
    }
}
