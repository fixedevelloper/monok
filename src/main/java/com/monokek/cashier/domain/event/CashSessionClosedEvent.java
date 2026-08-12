package com.monokek.cashier.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/** Raised by {@code CashSession#close} — port of the Z-report Laravel builds inline in {@code CashController::close}. */
public record CashSessionClosedEvent(
        Long sessionId, Long userId, BigDecimal closingAmount, BigDecimal expectedAmount, BigDecimal difference, Instant occurredAt) {

    public CashSessionClosedEvent(Long sessionId, Long userId, BigDecimal closingAmount, BigDecimal expectedAmount) {
        this(sessionId, userId, closingAmount, expectedAmount, closingAmount.subtract(expectedAmount), Instant.now());
    }
}
