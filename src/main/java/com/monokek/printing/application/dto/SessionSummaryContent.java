package com.monokek.printing.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** {@code PrintQueue.content}, deserialized — everything {@link com.monokek.printing.application.EscPosTicketRenderer} needs for a cash-session closing report (Z-report). */
public record SessionSummaryContent(
        Long sessionId,
        String storeName,
        String cashierName,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        BigDecimal openingAmount,
        BigDecimal totalSales,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        BigDecimal difference,
        List<PaymentBreakdown> paymentBreakdown,
        List<SoldItem> soldItems) {

    public record PaymentBreakdown(String method, BigDecimal total) {
    }

    public record SoldItem(String productName, int qty, BigDecimal total) {
    }
}
