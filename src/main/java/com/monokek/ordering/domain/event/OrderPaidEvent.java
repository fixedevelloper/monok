package com.monokek.ordering.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published once a payment is finalized, carrying everything
 * {@code printing.application.PrintQueueListener} needs to render a receipt
 * (item names/prices/modifiers grouped by round, table name, totals, payment
 * method) without calling back into {@code ordering} — same "fold the data
 * into the event" convention as {@link KitchenTicketRequestedEvent}.
 * Explicitly published from {@code OrderService#finalizePayment} (the only
 * call site of {@code Order#markPaid} in the codebase) rather than resolved
 * through {@code Order}'s {@code @DomainEvents} like {@link OrderStatusChangedEvent},
 * since {@code Order} itself is forbidden from reaching into {@code catalog}/
 * {@code floorplan} to resolve names — {@code OrderService} already can.
 *
 * <p>{@code rounds} groups items the same way the printed receipt does
 * ("--- SERVICE #1 ---" per round), not a flat item list.
 */
public record OrderPaidEvent(
        Long orderId,
        UUID orderUuid,
        Long branchId,
        String reference,
        String tableName,
        String serverName,
        List<RoundItems> rounds,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal total,
        String paymentMethod,
        BigDecimal amountReceived,
        BigDecimal changeDue,
        Instant occurredAt) {

    public OrderPaidEvent(
            Long orderId, UUID orderUuid, Long branchId, String reference, String tableName, String serverName, List<RoundItems> rounds,
            BigDecimal subtotal, BigDecimal tax, BigDecimal discount, BigDecimal total,
            String paymentMethod, BigDecimal amountReceived, BigDecimal changeDue) {
        this(orderId, orderUuid, branchId, reference, tableName, serverName, rounds, subtotal, tax, discount, total,
                paymentMethod, amountReceived, changeDue, Instant.now());
    }

    public record RoundItems(int roundNumber, List<TicketItem> items) {
    }

    public record TicketItem(String productName, int qty, BigDecimal unitPrice, BigDecimal total, List<String> modifierNames) {
    }
}
