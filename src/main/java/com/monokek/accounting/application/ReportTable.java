package com.monokek.accounting.application;

import java.time.LocalDate;
import java.util.List;

/**
 * Format-agnostic shape every accounting report renders down to: a title, a
 * period, column headers and typed rows. {@code ExcelReportWriter}/{@code
 * PdfReportWriter} each render exactly this shape — cell values are {@code
 * String}, {@code java.math.BigDecimal} or {@code Long}, so the Excel writer
 * can emit real numeric cells (an accountant needs to SUM() them, not
 * re-parse text) while the PDF writer just formats each type for display.
 */
public record ReportTable(String title, LocalDate startDate, LocalDate endDate, List<String> headers, List<List<Object>> rows) {
}
