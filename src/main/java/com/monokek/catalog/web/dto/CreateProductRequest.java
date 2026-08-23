package com.monokek.catalog.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * {@code image} is a plain URL/path string here, not a multipart file
 * upload like Laravel's — see the module's package-info for why file
 * upload/disk storage wasn't ported.
 */
public record CreateProductRequest(
        @NotNull Long categoryId,
        @NotBlank String name,
        String sku,
        @NotNull @DecimalMin("0") BigDecimal price,
        @PositiveOrZero BigDecimal purchasePrice,
        @PositiveOrZero BigDecimal incentiveAmount,
        @PositiveOrZero Integer stockCount,
        @PositiveOrZero Integer alertStock,
        @NotBlank String type,
        Boolean trackStock,
        String image,
        String description
) {
}
