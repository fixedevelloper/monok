package com.monokek.crm.domain.event;

import java.time.Instant;

/** Raised by {@code Customer#earnPoints}/{@code redeemPoints}. */
public record LoyaltyPointsChangedEvent(Long customerId, String type, int points, int newBalance, Instant occurredAt) {

    public LoyaltyPointsChangedEvent(Long customerId, String type, int points, int newBalance) {
        this(customerId, type, points, newBalance, Instant.now());
    }
}
