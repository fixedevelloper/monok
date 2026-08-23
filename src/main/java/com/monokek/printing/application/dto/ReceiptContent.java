package com.monokek.printing.application.dto;

import java.math.BigDecimal;
import java.util.List;

/** {@code PrintQueue.content}, deserialized — everything {@link com.monokek.printing.application.EscPosTicketRenderer} needs for a receipt. */
public record ReceiptContent(
        Long orderId,
        String orderUuid,
        String reference,
        String storeName,
        String storeAddress,
        String storePhone,
        String storeLogoUrl,
        String tableName,
        String serverName,
        String cashierName,
        List<RoundSection> rounds,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal total,
        String paymentMethod,
        BigDecimal amountReceived,
        BigDecimal changeDue) {

    public record RoundSection(int roundNumber, List<TicketItem> items) {
    }

    public record TicketItem(String productName, int qty, BigDecimal unitPrice, BigDecimal total, List<String> modifierNames) {
    }
}
