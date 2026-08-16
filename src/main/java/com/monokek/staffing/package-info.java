/**
 * Staff scheduling ("planning") and time-clock ("pointage") — new
 * functionality, no Laravel source to port (this was a complete gap: no
 * Shift/Schedule entity, controller, or screen existed anywhere). Two
 * related but independent concepts, mirroring {@code cashier}'s
 * {@code CashRegister}/{@code CashSession} split: {@code domain.Shift} is
 * the planned roster entry (who's expected to work when); {@code domain.TimeClockEntry}
 * is the actual clock-in/clock-out log, loosely linked to a same-day
 * {@code Shift} when one exists but never required — an employee can punch
 * in with no prior planned shift.
 *
 * <p>Depends only on {@code identity} ({@code CurrentUser} for the caller,
 * {@code UserDirectory} for display names) — same one-directional shape as
 * every other module referencing identity. {@code branchId}/{@code userId}
 * are plain {@code Long} columns throughout, never a JPA relation, matching
 * {@code cashier.CashRegister}/{@code CashSession}.
 *
 * <p>Self-service clock-in/out on a shared POS terminal identifies the
 * target employee by a 4-digit PIN looked up against monokek-identity's
 * {@code POST /api/auth/lookup-pin} — the frontend resolves the PIN to a
 * {@code userId} first, then calls this module's
 * {@code POST /api/staffing/clock/clock-in} with that already-resolved id;
 * this module never touches PINs itself.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Staffing")
package com.monokek.staffing;
