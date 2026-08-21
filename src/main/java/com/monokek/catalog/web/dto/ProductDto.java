package com.monokek.catalog.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Mirrors {@code App\Http\Resources\ProductResource}. */
public record ProductDto(
        Long id,
        Long categoryId,
        String categoryName,
        Long branchId,
        String sku,
        String name,
        String description,
        BigDecimal price,
        BigDecimal incentiveAmount,
        String formattedPrice,
        boolean active,
        boolean trackStock,
        int stockCount,
        int alertStock,
        String type,
        String image,
        List<ModifierDto> modifiers
) {
}
