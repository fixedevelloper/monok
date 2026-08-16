package com.monokek.printing.web.dto;

/** All fields optional, mirroring Laravel's {@code sometimes} validation rules. */
public record UpdatePrinterRequest(
        Long branchId,
        String name,
        String type,
        String connection,
        String ip,
        Integer port,
        Integer charPerLine,
        Boolean isActive,
        Boolean useBeep,
        Integer paperWidth,
        String location,
        String osPrinterName
) {
}
