/**
 * Cashier module: cash registers, cash sessions (X/Z reports) and payments.
 * Implemented end-to-end. Exposes {@link CashierFacade} at its root package
 * for {@code ordering} to record payments through — that dependency only
 * ever points {@code ordering -> cashier}, so {@code cashier} itself never
 * imports anything from {@code ordering}.
 *
 * <p>{@code application.CashierService#close} publishes {@code domain.event.CashSessionReportReadyEvent}
 * with everything {@code cashier} itself can compute (totals, per-payment-method
 * breakdown) — the Laravel "sold items summary" that would need {@code ordering}/
 * {@code catalog} data is added downstream by {@code ordering.application.SessionReportListener},
 * which re-publishes a fully-combined event for {@code printing} to consume.
 * That's the natural resolution of the one-directional dependency documented
 * above: the piece {@code cashier} can't compute is added by a module that's
 * already allowed to depend on it, not by {@code cashier} reaching backward.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Cashier")
package com.monokek.cashier;
