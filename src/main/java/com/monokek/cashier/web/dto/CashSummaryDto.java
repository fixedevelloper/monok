package com.monokek.cashier.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Mirrors {@code CashController::currentSummary}'s X-report response. */
public record CashSummaryDto(CashSessionDto session, List<PaymentBreakdownDto> paymentsDetail, BigDecimal totalSales) {
}
