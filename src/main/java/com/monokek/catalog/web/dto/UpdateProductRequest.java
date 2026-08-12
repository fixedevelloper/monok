package com.monokek.catalog.web.dto;

import java.math.BigDecimal;

/** All fields optional — only the ones present are applied, matching Laravel's {@code sometimes} rules. */
public record UpdateProductRequest(
        Long categoryId,
        String name,
        String sku,
        BigDecimal price,
        BigDecimal incentiveAmount,
        Integer stockCount,
        Integer alertStock,
        String type,
        Boolean trackStock,
        Boolean active,
        String image,
        String description
) {
}
