package com.monokek.printing.web.dto;

public record PrintQueueDto(
        Long id, Long printerId, String printerName, String printerIp, Integer printerPort, String printerOsPrinterName,
        String jobType, String content, int attempts, String status, String errorMessage, short priority
) {
}
