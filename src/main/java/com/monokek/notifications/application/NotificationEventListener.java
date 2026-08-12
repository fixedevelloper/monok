package com.monokek.notifications.application;

import com.monokek.ordering.domain.event.KitchenTicketRequestedEvent;
import com.monokek.ordering.domain.event.OrderCreatedEvent;
import com.monokek.ordering.domain.event.OrderStatusChangedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Translates {@code ordering}'s domain events into named SSE broadcasts —
 * the direct replacement for the frontend's old Echo channels: {@code orders}
 * (order.created/order.updated) and {@code kitchen.station.{id}} (ticket.created,
 * now filtered client-side on {@code stationId} in the payload instead of a
 * server-side per-station channel). {@code .round.added}/{@code .station.updated}
 * had no backend equivalent before this module and aren't ported — the pages
 * that used them already react to {@code order.updated}/{@code ticket.created}.
 */
@Component
public class NotificationEventListener {

    private final SseConnectionRegistry registry;

    public NotificationEventListener(SseConnectionRegistry registry) {
        this.registry = registry;
    }

    @ApplicationModuleListener
    void on(OrderCreatedEvent event) {
        registry.broadcastToBranch(event.branchId(), "order.created", Map.of(
                "orderId", event.orderId(),
                "orderUuid", event.orderUuid().toString(),
                "reference", event.reference(),
                "tableId", event.tableId()
        ));
    }

    @ApplicationModuleListener
    void on(OrderStatusChangedEvent event) {
        registry.broadcastToBranch(event.branchId(), "order.updated", Map.of(
                "orderId", event.orderId(),
                "orderUuid", event.orderUuid().toString(),
                "previousStatus", event.previousStatus(),
                "newStatus", event.newStatus()
        ));
    }

    @ApplicationModuleListener
    void on(KitchenTicketRequestedEvent event) {
        registry.broadcastToBranch(event.branchId(), "ticket.created", Map.of(
                "orderId", event.orderId(),
                "roundId", event.roundId(),
                "stationId", event.stationId(),
                "tableName", event.tableName() == null ? "" : event.tableName(),
                "serverName", event.serverName() == null ? "" : event.serverName()
        ));
    }
}
