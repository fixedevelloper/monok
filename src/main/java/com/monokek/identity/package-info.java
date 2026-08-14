/**
 * Identity &amp; access module — now a thin client of the standalone
 * {@code monokek-identity} service, which owns user management and
 * authentication (extracted so pms-modulith, the other consumer of identity
 * data, never depends on this app's uptime). What's left here:
 * <ul>
 *   <li>{@code infrastructure.security} — validates the bearer JWTs
 *       monokek-identity issues (JWKS-based, {@link
 *       org.springframework.security.oauth2.jwt.NimbusJwtDecoder}) and maps
 *       their claims onto {@link com.monokek.identity.CurrentUser}, this
 *       module's published principal type — no local {@code User} table,
 *       no password checks, nothing stateful</li>
 *   <li>{@code infrastructure.client} — {@code UserDirectory}'s
 *       implementation is now an HTTP call to monokek-identity's
 *       {@code GET /internal/users} instead of a local JPA query</li>
 *   <li>{@code domain}/{@code domain.event} — {@code Device} (POS/kitchen/
 *       mobile terminal registry, never depended on {@code User} directly)
 *       stays here; the four staff/login domain events
 *       ({@code StaffCreatedEvent} and friends) stay as plain record types
 *       consumed by {@code settings.application.ActivityLogListener}, now
 *       fed by an HTTP webhook from monokek-identity instead of an
 *       in-process Spring Modulith publication — see that listener and
 *       monokek-identity's {@code ActivityNotifier}</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(displayName = "Identity")
package com.monokek.identity;
