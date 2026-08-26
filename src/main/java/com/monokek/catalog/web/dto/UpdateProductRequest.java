package com.monokek.catalog.web.dto;

import java.math.BigDecimal;

/** All fields optional — only the ones present are applied, matching Laravel's {@code sometimes} rules.
 * {@code stockCount} is deliberately absent: the product edit form can no longer move stock directly —
 * see {@code ProductController#adjustStock} for the only path left (admin-only, always traced by a
 * {@code ProductStockMovement}). {@code alertStock} stays here since it's a config threshold, not a movement. */
public record UpdateProductRequest(
        Long categoryId,
        String name,
        String sku,
        BigDecimal price,
        BigDecimal purchasePrice,
        BigDecimal incentiveAmount,
        Integer alertStock,
        String type,
        Boolean trackStock,
        Boolean active,
        String image,
        String description
) {
}
