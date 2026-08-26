/**
 * Customer relationship module: customers, coupons and loyalty points.
 *
 * <p>Unlike every other module ported so far, {@code crm} had essentially
 * nothing to port from Laravel: the only real usage anywhere in the source
 * (an inline {@code Customer::firstOrCreate} in {@code ReservationController})
 * is already covered by {@link CustomerDirectory}, used by {@code ordering}.
 * {@code Coupon} and {@code LoyaltyTransaction} had models, a schema and
 * boilerplate API resources, but zero routes, controllers, or business
 * rules anywhere in the Laravel app. Everything under {@code application}/
 * {@code web} here — customer admin CRUD, coupon creation/validation, the
 * earn/redeem loyalty ledger — is new functionality built to the same
 * DDD/event-driven pattern as the ported modules, not a translation of
 * existing behavior. {@code Customer} stages {@code LoyaltyPointsChangedEvent}
 * the same way monokek-identity's {@code User} aggregate stages its events; {@code settings}
 * picks it up like it does every other module's events.
 *
 * <p>{@link CouponCatalog} is the same shape of dependency as {@link CustomerDirectory}, added
 * later so {@code ordering.application.OrderService} can price/redeem a coupon against a specific
 * order (expiry, minimum amount, usage cap) without reaching into {@code crm.domain} — implemented
 * by the same {@code CouponService} that backs the admin coupon CRUD, since pricing a coupon
 * against an order and validating it are the same business rule, not two separate concerns.
 */
@org.springframework.modulith.ApplicationModule(displayName = "CRM")
package com.monokek.crm;
