package com.monokek.cashier.web.dto;

import java.math.BigDecimal;

public record CashSessionDto(
        Long id,
        Long registerId,
        String registerName,
        BigDecimal openingAmount,
        BigDecimal closingAmount,
        String openedAt,
        String closedAt,
        BigDecimal expectedAmount,
        String note
) {
}
