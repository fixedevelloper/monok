package com.monokek.reporting.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record ClosingReportResponse(
        String cashier,
        String date,
        List<PaymentMethodTotal> breakdown,
        BigDecimal total
) {
}
