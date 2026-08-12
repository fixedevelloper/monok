/**
 * Floor plan module: floors and restaurant tables. Implemented end-to-end,
 * porting {@code App\Http\Controllers\Api\Pos\TableController} — a
 * genuinely used, genuinely buggy controller (unlike {@code crm}/{@code company}'s
 * near-total absence of Laravel code). Three deliberate deviations, each
 * documented at its call site rather than silently applied:
 * <ul>
 *   <li>Table status vocabulary unified on {@code free}/{@code occupied}/
 *       {@code billing}/{@code maintenance} — Laravel's {@code updateStatus}
 *       validated {@code available} instead of {@code free}, which conflicts
 *       with the seeded/default status and with what
 *       {@code ordering.TableDirectory} already writes.</li>
 *   <li>{@code floors()}'s response drops the {@code currentOrder}/{@code total}
 *       enrichment: computing it would need this module to depend on
 *       {@code ordering}, which already depends on this module (through
 *       {@link TableDirectory}) — the reverse dependency would cycle. The
 *       same data is one call away through {@code ordering}'s existing
 *       {@code GET /pos/tables/{id}/active-order}.</li>
 *   <li>{@code transfer()} moved to {@code ordering.web.OrderController}/
 *       {@code OrderService#transferTable} — it mutates {@code Order.tableId},
 *       so for the same one-directional-dependency reason it can't live
 *       here. Its Laravel implementation also calls a
 *       {@code $fromTable->activeOrder()} method that doesn't exist
 *       anywhere in the codebase (dead code — fixed in the port).</li>
 * </ul>
 *
 * <p>{@code application.DefaultTableProvisioner} adds a one-directional
 * {@code floorplan -> company} edge, reacting to
 * {@code company.domain.event.BranchCreatedEvent} to seed every new
 * branch's fallback table for walk-in customers when the floor is full.
 * {@code company} never depends back on {@code floorplan}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Floor plan")
package com.monokek.floorplan;
