package com.monokek.cashier.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/** Raised by {@code CashSession#open}. */
public record CashSessionOpenedEvent(Long sessionId, Long registerId, Long userId, BigDecimal openingAmount, Instant occurredAt) {

    public CashSessionOpenedEvent(Long sessionId, Long registerId, Long userId, BigDecimal openingAmount) {
        this(sessionId, registerId, userId, openingAmount, Instant.now());
    }
}
