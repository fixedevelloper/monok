package com.monokek.reporting.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsResponse(
        String date,
        BigDecimal totalRevenue,
        List<PaymentMethodTotal> revenueByMethod,
        long ordersCount,
        List<TopProduct> topProducts
) {
    public record TopProduct(String product, long qty) {
    }
}
