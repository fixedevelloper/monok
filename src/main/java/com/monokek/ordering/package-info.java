/**
 * Ordering module: the order lifecycle (rounds sent to the kitchen, line
 * items, status history), reservations and waiter commissions. Implemented
 * end-to-end, alongside {@code identity}, as one of the reference modules
 * for the rest of the port.
 *
 * <p>Published interfaces at this root package: {@link RoundKitchenView}
 * (read model for {@code kitchen}) and {@link OrderRoundStatusUpdater}
 * (write-back target for {@code kitchen}) — see {@code kitchen}'s
 * package-info for why that dependency only ever points one way.
 * {@code inventory} used to have a third one here, {@code OrderLineItems},
 * just to re-fetch an order's sold items after being notified it was paid —
 * deleted once {@code domain.event.OrderStatusChangedEvent} started
 * carrying that same item list directly in its payload, so a plain event
 * listener is now the only coupling {@code inventory} needs. {@code ordering}
 * never depends back on either {@code kitchen} or {@code inventory}.
 *
 * <p>{@code application.SessionReportListener} extends the existing
 * one-directional {@code ordering -> cashier} edge (previously only
 * {@code CashierFacade}) to also cover {@code cashier}'s published domain
 * events: it reacts to {@code CashSessionReportReadyEvent}, adds the
 * sold-items summary {@code cashier} can't compute itself, and re-publishes
 * {@code domain.event.SessionReportReadyEvent} for {@code printing} — same
 * "fold the data into the event, resolve it where the dependency is already
 * allowed" shape as everything else above.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Ordering")
package com.monokek.ordering;
