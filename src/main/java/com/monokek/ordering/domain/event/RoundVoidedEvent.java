package com.monokek.ordering.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published once a round is voided (cashier action, manager-PIN approved — see
 * {@code ordering.application.OrderService#voidRound} and {@code identity.ManagerAuthClient}).
 * Consumed by {@code kitchen.application.KitchenTicketListener} (cancels any ticket already
 * created for the round) and {@code settings.application.ActivityLogListener} (the admin audit
 * trail this whole mechanism exists for).
 */
public record RoundVoidedEvent(
        Long orderId, UUID orderUuid, String orderReference, Long branchId, Long roundId, int roundNumber,
        String reason, Long voidedByCashierUserId, Long approvedByManagerUserId, Instant occurredAt) {

    public RoundVoidedEvent(
            Long orderId, UUID orderUuid, String orderReference, Long branchId, Long roundId, int roundNumber,
            String reason, Long voidedByCashierUserId, Long approvedByManagerUserId) {
        this(orderId, orderUuid, orderReference, branchId, roundId, roundNumber, reason,
                voidedByCashierUserId, approvedByManagerUserId, Instant.now());
    }
}
