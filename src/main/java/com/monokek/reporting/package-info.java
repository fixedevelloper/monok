/**
 * Port of {@code App\Http\Controllers\Api\Admin\ReportController}: dashboard
 * KPIs, sales-by-category, a cashier's daily closing summary, and the
 * admin analytics dashboard. Deliberately deferred until every other module
 * was done, since it cuts across all of them.
 *
 * <p><b>Architectural exception, and why:</b> every other module owns its
 * tables and exposes a narrow published interface (e.g. {@code
 * catalog.ProductCatalog}, {@code identity.UserDirectory}) for anything
 * another module needs. That pattern breaks down here: these four endpoints
 * are ad-hoc SQL aggregations (SUM/GROUP BY/JOIN across {@code orders},
 * {@code order_items}, {@code order_rounds}, {@code payments}, {@code
 * payment_methods}, {@code products}, {@code categories}, {@code users}) —
 * composing them from narrow cross-module method calls would mean pulling
 * full result sets into Java and aggregating by hand, which is both slower
 * and more code than the SQL it's replacing. {@code application.ReportingService}
 * instead runs read-only native SQL directly against the shared schema via
 * {@code NamedParameterJdbcTemplate}, addressing tables by name rather than
 * importing other modules' JPA entities. This module therefore has no
 * {@code domain} package of its own and, from Spring Modulith's point of
 * view, no Java-level dependency on any other module — {@code
 * ModularityTests} can't see a table read the way it sees a type reference.
 * That's acceptable specifically because this is the CQRS read side of an
 * already-shared MySQL schema: no writes happen here, no domain invariants
 * are bypassed, and nothing here can put another module's aggregate in an
 * inconsistent state. This exception should not be treated as precedent for
 * regular business logic — every other module's rule (data access only
 * through your own domain, cross-module needs go through a published
 * interface or an event) still stands everywhere else.
 *
 * <p><b>Real bugs found and fixed rather than replicated</b> (the original
 * Laravel would throw a SQL error on two of these four endpoints):
 * <ul>
 *   <li>{@code dashboardStats()} selected {@code orders.total_amount} and
 *       {@code orders.payment_method} — neither column exists. The real
 *       amount column is {@code orders.total}; payment method is only known
 *       per-payment via {@code payments.payment_method_id -> payment_methods}.
 *       Rebuilt the "cash vs mobile" split as a generic per-method
 *       breakdown from {@code payments}, since hardcoding Laravel's
 *       {@code cash}/{@code momo}/{@code orange} bucket names would silently
 *       drop any other method (this schema's seed data has no
 *       {@code orange} row at all). It also selected {@code
 *       order_items.quantity}, which doesn't exist either — the real column
 *       is {@code qty}.</li>
 *   <li>{@code salesByCategory()} selected {@code order_items.subtotal} —
 *       doesn't exist; the real column is {@code order_items.total}.</li>
 *   <li>{@code closingReport()} had the same {@code total_amount}/{@code
 *       payment_method} problem as {@code dashboardStats()}. Rebuilt around
 *       the same {@code payments} join, scoped to the current user's own
 *       completed orders for today (Laravel's original intent: a waiter's/
 *       cashier's own end-of-day summary, distinct from {@code
 *       cashier.CashSession}'s Z-report which is scoped by session rather
 *       than by user-and-calendar-day).</li>
 *   <li>{@code getAnalytics()} was already almost entirely correct against
 *       the real schema (unlike the other three) — ported with only the
 *       column names double-checked, no structural changes. Its hardcoded
 *       {@code food_cost => 32} Laravel placeholder was replaced with a real
 *       computation ({@code foodCostPercent(...)}): cost of goods sold, derived
 *       from each sold product's recipe and each ingredient's most recent
 *       purchase price, as a percentage of {@code totalSales}. Ingredients
 *       have no stored current unit cost, so this is an approximation off the
 *       latest known purchase price rather than a weighted average.</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(displayName = "Reporting")
package com.monokek.reporting;
