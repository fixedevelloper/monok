package com.monokek.printing.web.dto;

public record PrinterDto(
        Long id,
        Long branchId,
        String name,
        String type,
        String connection,
        String location,
        String ip,
        int port,
        int charPerLine,
        boolean active,
        int paperWidth,
        boolean useBeep,
        String osPrinterName
) {
}
