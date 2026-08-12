/**
 * Accounting module: OHADA-style bookkeeping exports (Excel/PDF) for the
 * store's expert-comptable — the four reports {@code AccountingPage.tsx}
 * (frontend) has always rendered a UI for, against an endpoint
 * (`/api/accounting/*`) that never existed server-side until now.
 *
 * <p>Architecturally the same deliberate exception as {@code reporting}: no
 * domain package, no owned tables, read-only native SQL against the shared
 * schema via {@code NamedParameterJdbcTemplate} rather than composing
 * results from other modules' narrow published interfaces (these are
 * ad-hoc SUM/GROUP BY/JOIN aggregations across {@code orders}, {@code
 * order_items}, {@code order_rounds}, {@code payments}, {@code
 * payment_methods}, {@code products}, {@code categories} — pulling that
 * through cross-module method calls would mean aggregating full result sets
 * by hand in Java). See {@code reporting.package-info} for the full
 * rationale; same one-time exception, not a precedent for regular business
 * logic elsewhere in this codebase.
 *
 * <p>{@code application.AccountingService} builds a format-agnostic {@code
 * ReportTable} (headers + typed rows) per report; {@code
 * application.ExcelReportWriter}/{@code PdfReportWriter} render that same
 * table to real {@code .xlsx} (Apache POI) or {@code .pdf} (PDFBox) bytes —
 * one renderer per format, not one per report, since every report is just a
 * titled table.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Accounting")
package com.monokek.accounting;
