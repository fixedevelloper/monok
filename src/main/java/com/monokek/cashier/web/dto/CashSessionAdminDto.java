package com.monokek.cashier.web.dto;

import java.math.BigDecimal;

/** Richer than {@link CashSessionDto} (branch/cashier identity, open/closed state) — backs the
 * admin "browse shifts to export" screen, where {@code CashSessionDto} (the caller's own session)
 * doesn't need any of that. */
public record CashSessionAdminDto(
        Long id,
        Long registerId,
        String registerName,
        Long branchId,
        Long cashierUserId,
        String cashierName,
        BigDecimal openingAmount,
        BigDecimal closingAmount,
        BigDecimal expectedAmount,
        String openedAt,
        String closedAt,
        boolean open
) {
}
