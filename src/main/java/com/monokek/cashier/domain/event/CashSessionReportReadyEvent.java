package com.monokek.cashier.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Published explicitly from {@code CashierService#close} (not resolved
 * through {@code CashSession}'s {@code @DomainEvents} like
 * {@link CashSessionClosedEvent}, which stays untouched — {@code ActivityLogListener}
 * keeps consuming it exactly as before) — carries everything {@code cashier}
 * already computes for the Z-report (totals, per-payment-method breakdown)
 * so {@code ordering.application.SessionReportListener} only needs to add
 * the one piece {@code cashier} can't resolve itself (sold items, which live
 * in {@code ordering}/{@code catalog} — see {@code cashier}'s package-info
 * and README for why that resolution can't happen here).
 */
public record CashSessionReportReadyEvent(
        Long sessionId,
        Long branchId,
        Long cashierUserId,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        BigDecimal openingAmount,
        BigDecimal totalSales,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        BigDecimal difference,
        String note,
        List<PaymentBreakdown> paymentBreakdown,
        Instant occurredAt) {

    public CashSessionReportReadyEvent(
            Long sessionId, Long branchId, Long cashierUserId, LocalDateTime openedAt, LocalDateTime closedAt,
            BigDecimal openingAmount, BigDecimal totalSales, BigDecimal expectedAmount, BigDecimal actualAmount,
            BigDecimal difference, String note, List<PaymentBreakdown> paymentBreakdown) {
        this(sessionId, branchId, cashierUserId, openedAt, closedAt, openingAmount, totalSales, expectedAmount,
                actualAmount, difference, note, paymentBreakdown, Instant.now());
    }

    public record PaymentBreakdown(String method, BigDecimal total) {
    }
}
