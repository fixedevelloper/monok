/**
 * Printing module: printers (CRUD + a real ESC/POS test ticket over the
 * network) and the print job queue. Implemented end-to-end for everything
 * that's actual server-side business logic. Depends on {@code ordering}
 * (its {@code domain.event} named interface — {@code KitchenTicketRequestedEvent}/
 * {@code OrderPaidEvent}/{@code SessionReportReadyEvent}) and on {@code settings}
 * ({@code StoreSettings}, for the receipt header + logo) — same shape as
 * {@code kitchen} and {@code inventory} depending on {@code ordering}'s events.
 *
 * <p>{@code application.PrintQueueListener} reacts to those three events,
 * builds the real ticket content ({@code application.dto.KitchenTicketContent}/
 * {@code ReceiptContent}/{@code SessionSummaryContent} — item names, prices,
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
 * <p><b>Still out of scope:</b> USB printers (no server-side network path to
 * them — jobs for them stay {@code pending}, exactly as before, for
 * whatever LAN worker polls {@code GET /api/print-queue/pending} and reports
 * back via {@code mark-success}/{@code mark-failed}), and automatic retry of
 * failed network jobs ({@code attempts} is incremented but nothing re-queues).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Printing")
package com.monokek.printing;
