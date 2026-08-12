package com.monokek.ordering.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Raised the first time a round is sent for a table with no open order yet. */
public record OrderCreatedEvent(
        Long orderId, UUID orderUuid, String reference, Long branchId, Long tableId, Long waiterUserId, Instant occurredAt) {

    public OrderCreatedEvent(Long orderId, UUID orderUuid, String reference, Long branchId, Long tableId, Long waiterUserId) {
        this(orderId, orderUuid, reference, branchId, tableId, waiterUserId, Instant.now());
    }
}
