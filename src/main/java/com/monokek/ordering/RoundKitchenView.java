package com.monokek.ordering;

import java.util.List;
import java.util.Optional;

/**
 * Published interface: what {@code kitchen} needs to render a ticket
 * (order reference, table, the round's line items) without reaching into
 * {@code ordering.domain}. Mirrors what Laravel's {@code KitchenTicketResource}
 * pulls off {@code $this->order}/{@code $this->round} through eager-loaded
 * relations — here resolved explicitly instead of implicitly.
 */
public interface RoundKitchenView {

    Optional<RoundSnapshot> findRound(Long roundId);

    record RoundSnapshot(
            Long id, int roundNumber, String status, Long orderId, String orderReference, String tableName, List<ItemSnapshot> items) {
    }

    record ItemSnapshot(Long id, Long productId, int qty, List<Long> modifierItemIds) {
    }
}
