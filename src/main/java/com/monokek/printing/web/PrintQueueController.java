package com.monokek.printing.web;

import com.monokek.printing.application.PrintQueueService;
import com.monokek.printing.web.dto.PrintQueueDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Port of the inline print-queue closures in {@code routes/api.php} — kept
 * public (no auth), matching the Laravel source, so a LAN print worker can
 * poll it without a token. {@code SecurityConfig} already permits these
 * exact paths. {@code dispatch-network} (server-side ESC/POS rendering) is
 * not ported — see the module's package-info.
 */
@RestController
@RequestMapping("/api/print-queue")
public class PrintQueueController {

    private final PrintQueueService printQueueService;

    public PrintQueueController(PrintQueueService printQueueService) {
        this.printQueueService = printQueueService;
    }

    @GetMapping("/pending")
    public List<PrintQueueDto> pending() {
        return printQueueService.pending();
    }

    @PostMapping("/{jobId}/mark-success")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markSuccess(@PathVariable Long jobId) {
        printQueueService.markSuccess(jobId);
    }

    @PostMapping("/{jobId}/mark-failed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markFailed(@PathVariable Long jobId) {
        printQueueService.markFailed(jobId);
    }
}
