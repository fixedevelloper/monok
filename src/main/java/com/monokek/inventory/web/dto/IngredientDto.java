package com.monokek.inventory.web.dto;

import java.math.BigDecimal;

/** Mirrors {@code App\Http\Resources\IngredientResource}. */
public record IngredientDto(Long id, String name, BigDecimal stock, BigDecimal alertQty, String unit, boolean isLowStock) {
}
