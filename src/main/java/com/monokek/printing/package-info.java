/**
 * Printing module: printers (CRUD + a real ESC/POS test ticket over the
 * network) and the print job queue. Implemented end-to-end for everything
 * that's actual server-side business logic. Depends on {@code ordering}
 * (its {@code domain.event} named interface — {@code KitchenTicketRequestedEvent}/
 * {@code OrderPaidEvent}/{@code SessionReportReadyEvent}), on {@code crm}
 * (its {@code domain.event} named interface — {@code CouponPrintRequestedEvent},
 * raised on demand from the CRM screen rather than tied to an order) and on
 * {@code settings} ({@code StoreSettings}, for the receipt header + logo) —
 * same shape as {@code kitchen} and {@code inventory} depending on {@code ordering}'s events.
 *
 * <p>{@code application.PrintQueueListener} reacts to those four events,
 * builds the real ticket content ({@code application.dto.KitchenTicketContent}/
 * {@code ReceiptContent}/{@code SessionSummaryContent}/{@code CouponContent} — item names, prices,
 * table, payment method, per-round grouping; no more ids-only placeholder),
 * enqueues the job, and — for network printers only — hands it straight to
 * {@code application.NetworkPrintDispatcher}, which renders it with
 * {@code application.EscPosTicketRenderer} (ported line-for-line from a
 * reference {@code mike42/escpos-php} implementation: {@code Style}-based
 * formatting, a QR code on receipts, the store logo via {@code infrastructure.LogoImageLoader}
 * + {@code escpos-coffee}'s image support, a cash-drawer pulse) and pushes
 * it over TCP via {@code escpos-coffee} (same probe-then-{@code TcpIpOutputStream}
 * pattern as {@code PrinterService::testConnection}), synchronously, right
 * when the job is queued — no separate scheduler.
 *
 * <p>USB jobs stay queued ({@code pending}) exactly as before — there's still
 * no server-side network path to a USB printer — but {@code application.PrintQueueListener}
 * also publishes {@code domain.event.PrintJobQueuedEvent} (a {@code @NamedInterface})
 * for them, which {@code notifications.application.NotificationEventListener}
 * turns into a real-time {@code print-job-queued} SSE push to the owning
 * branch's Tauri app; {@code GET /api/print-queue/pending?branchId=...} (now
 * branch-scoped) remains as a catch-up fetch for whatever was queued while
 * that app was offline. {@code application.PrintRetryScheduler} retries both
 * kinds of failed job: network jobs by re-dispatching them itself, USB jobs
 * by re-publishing {@code PrintJobQueuedEvent} so the SSE push reaches
 * whichever app is (or becomes) connected — see its own javadoc.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Printing")
package com.monokek.printing;
