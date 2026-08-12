package com.monokek.ordering.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Raised on every {@code Order#changeStatus} — payment, service,
 * cancellation... Carries the full sold-item list (product id + qty, one
 * entry per {@code OrderItem}) so a listener like
 * {@code inventory.application.StockDeductionListener} never needs a second,
 * separate call back into {@code ordering} just to find out what was sold —
 * the event alone is enough. Before this, {@code inventory} depended on both
 * this event type <em>and</em> a narrow {@code ordering.OrderLineItems}
 * published interface just to re-fetch the same order; folding the data into
 * the event removed that second dependency entirely.
 */
public record OrderStatusChangedEvent(
        Long orderId, UUID orderUuid, Long branchId, String previousStatus, String newStatus, Long changedByUserId,
        List<SoldItem> items, Instant occurredAt) {

    public OrderStatusChangedEvent(
            Long orderId, UUID orderUuid, Long branchId, String previousStatus, String newStatus, Long changedByUserId, List<SoldItem> items) {
        this(orderId, orderUuid, branchId, previousStatus, newStatus, changedByUserId, items, Instant.now());
    }

    public record SoldItem(Long productId, int qty) {
    }
}
