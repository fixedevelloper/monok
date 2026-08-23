package com.monokek.reporting.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsResponse(
        String startDate,
        String endDate,
        BigDecimal totalSales,
        long ordersCount,
        BigDecimal averageCart,
        BigDecimal foodCostPercent,
        BigDecimal grossMargin,
        BigDecimal grossMarginPercent,
        List<HourlyFlow> hourlyFlow,
        List<WaiterPerformance> waiterPerformance,
        List<PaymentMethodTotal> paymentsByMethod,
        List<SalesOverTime> salesOverTime,
        List<TopProduct> topProducts
) {
    public record HourlyFlow(int hour, long count) {
    }

    public record WaiterPerformance(String waiter, BigDecimal sales, long orders) {
    }

    public record SalesOverTime(String date, BigDecimal total) {
    }

    public record TopProduct(String product, long qty, BigDecimal revenue) {
    }
}
