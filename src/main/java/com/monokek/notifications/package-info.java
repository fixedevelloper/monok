/**
 * Real-time notifications module: pushes server→client updates over
 * Server-Sent Events instead of the WebSocket (Laravel Echo/Reverb)
 * infrastructure the frontend used to depend on — notifications only ever
 * flow one direction, so SSE (plain HTTP, native browser reconnection) is a
 * strictly simpler fit than a full duplex WebSocket.
 *
 * <p>Depends on {@code ordering.domain.event} and {@code printing.domain.event}
 * (both {@code @NamedInterface}s), same one-directional shape as {@code printing}/
 * {@code kitchen}/{@code inventory} depending on {@code ordering}'s events —
 * {@code application.NotificationEventListener} translates
 * {@code OrderCreatedEvent}/{@code OrderStatusChangedEvent}/{@code KitchenTicketRequestedEvent}/
 * {@code PrintJobQueuedEvent} into named SSE broadcasts.
 *
 * <p>Native {@code EventSource} can't send an {@code Authorization} header,
 * so the stream is guarded by a short-lived, single-use ticket instead of
 * the JWT filter: {@code POST /api/sse/ticket} (normal JWT auth) issues a
 * ticket, {@code GET /api/sse/stream?ticket=...} (public — see
 * {@code SecurityConfig}) consumes it. {@code branchId} is passed explicitly
 * by the client when requesting a ticket — same as every other branch-scoped
 * write in this codebase (table/register), never resolved from the JWT,
 * which carries no branch claim.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Notifications")
package com.monokek.notifications;
