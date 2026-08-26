package com.monokek.reporting.web.dto;

import java.math.BigDecimal;

/** One row of the unified stock-movement history — {@code subjectType} is "ingredient" or
 * "product" depending on which of the two underlying tables (owned by {@code inventory} and
 * {@code catalog} respectively) it came from; {@code qty} is already signed (negative for "out").
 * {@code purchase} is derived from the reason text (see {@code StockMovementReportingService})
 * rather than a real database column — a supplier purchase is still fundamentally an "in"
 * movement, this just lets the UI offer it as its own filter. */
public record StockMovementRowDto(
        String occurredAt, String subjectType, String subjectName, String type,
        BigDecimal qty, String reason, String authorName, boolean purchase) {
}
