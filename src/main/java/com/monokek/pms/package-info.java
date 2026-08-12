/**
 * Bridges to pms-modulith — the separate hotel PMS service — so a restaurant
 * order can be billed to a guest's room folio instead of collected at the
 * till. Used by {@code ordering.OrderService#finalizePayment} when the
 * request's payment method is {@code room_charge}.
 *
 * <p>This is a real HTTP call to another application (base URL
 * {@code app.pms.api-url}), not an in-process module dependency. It reuses
 * the requesting cashier's own bearer token: pms-modulith accepts it
 * directly because both services are configured with the same JWT signing
 * secret (see pms-modulith's {@code SecurityConfig} — it validates tokens
 * issued by this app, it never issues its own).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Pms Integration")
package com.monokek.pms;
