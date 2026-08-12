package com.monokek.cashier.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirrors {@code CashController::close}'s Z-report response (this HTTP
 * response, for the closing screen). The "sold items summary" and the
 * actual print job are handled separately: {@code CashierService#close}
 * also publishes {@code domain.event.CashSessionReportReadyEvent}, which
 * {@code ordering.application.SessionReportListener} enriches with sold
 * items before {@code printing} renders and dispatches the physical
 * receipt — see {@code cashier}'s package-info.
 */
public record CloseReportDto(String message, Report report) {

    public record Report(
            String openedAt,
            String closedAt,
            BigDecimal openingAmount,
            BigDecimal totalPayments,
            BigDecimal expectedTotal,
            BigDecimal actualTotal,
            BigDecimal difference,
            String note,
            List<PaymentBreakdownDto> paymentsDetail
    ) {
    }
}
