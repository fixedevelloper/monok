package com.monokek.ordering.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Published by {@code ordering.application.SessionReportListener} after
 * enriching {@code cashier.domain.event.CashSessionReportReadyEvent} with
 * the one thing {@code cashier} can't resolve itself — the sold-items
 * summary (needs {@code ordering}/{@code catalog}) — plus the cashier's
 * display name (needs {@code identity}). {@code printing.application.PrintQueueListener}
 * is already subscribed to this package, so no new dependency is needed
 * there to consume it.
 */
public record SessionReportReadyEvent(
        Long sessionId,
        Long branchId,
        String cashierName,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        BigDecimal openingAmount,
        BigDecimal totalSales,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        BigDecimal difference,
        String note,
        List<PaymentBreakdown> paymentBreakdown,
        List<SoldItem> soldItems,
        Instant occurredAt) {

    public SessionReportReadyEvent(
            Long sessionId, Long branchId, String cashierName, LocalDateTime openedAt, LocalDateTime closedAt,
            BigDecimal openingAmount, BigDecimal totalSales, BigDecimal expectedAmount, BigDecimal actualAmount,
            BigDecimal difference, String note, List<PaymentBreakdown> paymentBreakdown, List<SoldItem> soldItems) {
        this(sessionId, branchId, cashierName, openedAt, closedAt, openingAmount, totalSales, expectedAmount,
                actualAmount, difference, note, paymentBreakdown, soldItems, Instant.now());
    }

    public record PaymentBreakdown(String method, BigDecimal total) {
    }

    public record SoldItem(String productName, int qty, BigDecimal total) {
    }
}
