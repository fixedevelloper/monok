package com.monokek.inventory.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Purchase orders are always "received" immediately (see {@code PurchaseOrder#receive}), so this
 * one shape doubles as both the list row and the detail — no separate {@code show}. */
public record PurchaseOrderDto(
        Long id, Long supplierId, String supplierName, String status, BigDecimal total, String createdAt, List<Line> items) {

    /** Exactly one of {@code ingredientName}/{@code productName} is non-null, matching whichever of
     * {@code ingredientId}/{@code productId} the line references. */
    public record Line(
            Long id, Long ingredientId, String ingredientName, Long productId, String productName,
            BigDecimal qty, BigDecimal price) {
    }
}
