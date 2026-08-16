package com.monokek.printing.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePrinterRequest(
        @NotNull Long branchId,
        @NotBlank String name,
        @NotBlank String type,       // escpos, label
        @NotBlank String connection, // usb, network
        String ip,
        Integer port,
        Integer charPerLine,
        Boolean useBeep,
        Integer paperWidth,          // 58, 80
        @NotBlank String location,   // receipt, kitchen, bar, pizza, delivery
        String osPrinterName         // CUPS/Windows printer name — usb only
) {
}
