/**
 * Kitchen Display System module: preparation stations and tickets.
 * {@code kitchen} depends on {@code ordering} one way only:
 * {@code application.KitchenTicketListener} reacts to
 * {@code ordering.domain.event} to create/close tickets, and
 * {@code application.KitchenTicketService} reads
 * {@code ordering.RoundKitchenView} and writes back through
 * {@code ordering.OrderRoundStatusUpdater} — both published interfaces, both
 * called synchronously since a kitchen staff action needs its result
 * (the resolved round status) in the same HTTP response. {@code ordering}
 * never depends on {@code kitchen}.
 *
 * <p>{@link KitchenStationDirectory} is the other direction: a published
 * interface at this root package that {@code printing} calls to resolve a
 * station's type into the matching {@code printing.domain.Printer#location}
 * (e.g. a BAR station's tickets need the branch's {@code "bar"} printer, not
 * its {@code "kitchen"} one) — same "resolve where the dependency is already
 * allowed" shape as {@code ordering}'s published interfaces above.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Kitchen")
package com.monokek.kitchen;
