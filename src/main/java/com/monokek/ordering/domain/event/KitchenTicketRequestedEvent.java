package com.monokek.ordering.domain.event;

import java.time.Instant;
import java.util.List;

/**
 * Published once per (round, kitchen station) pair that needs a ticket — a
 * ticket groups every item of a round headed to the same station, exactly
 * like Laravel's {@code sendRound} grouping items by
 * {@code category.kitchen_station_id} before creating one {@code KitchenTicket}
 * per group. Consumed by {@code kitchen.application.KitchenTicketListener}
 * (only reads {@code stationId}/{@code orderId}/{@code roundId}/{@code note})
 * and {@code printing.application.PrintQueueListener} (queues the kitchen
 * printer's copy of the same ticket — needs {@code tableName}/{@code serverName}/
 * {@code items} too, to actually render it).
 */
public record KitchenTicketRequestedEvent(
        Long orderId, Long roundId, Long branchId, Long stationId, String note, Instant occurredAt,
        String tableName, List<TicketItem> items, String serverName) {

    public KitchenTicketRequestedEvent(
            Long orderId, Long roundId, Long branchId, Long stationId, String note,
            String tableName, List<TicketItem> items, String serverName) {
        this(orderId, roundId, branchId, stationId, note, Instant.now(), tableName, items, serverName);
    }

    public record TicketItem(String productName, int qty, List<String> modifierNames) {
    }
}
