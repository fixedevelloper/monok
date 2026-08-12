package com.monokek.ordering;

/**
 * Published interface: {@code kitchen} calls this synchronously — not via an
 * event — right after aggregating a round's tickets, because the HTTP
 * response to the kitchen staff's status update needs the resulting round
 * status immediately (see {@code KitchenTicketService#updateTicketStatus}
 * and {@code TicketController}'s {@code round_status} response field).
 *
 * <p>This deliberately keeps the {@code kitchen} → {@code ordering}
 * dependency one-directional: {@code kitchen} already depends on
 * {@code ordering} to know when to create a ticket
 * ({@code KitchenTicketRequestedEvent}), so writing the round status back
 * through another {@code ordering}-owned entry point keeps the module graph
 * acyclic. An {@code ordering} listener reacting to a {@code kitchen} event
 * here would create exactly the cycle Spring Modulith's
 * {@code ApplicationModules.verify()} rejects.
 */
public interface OrderRoundStatusUpdater {

    void applyKitchenRoundStatus(Long orderId, Long orderRoundId, String resolvedStatus);
}
