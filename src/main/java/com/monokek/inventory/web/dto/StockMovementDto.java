package com.monokek.inventory.web.dto;

import java.math.BigDecimal;

/** Mirrors {@code App\Http\Resources\StockMovementResource}. */
public record StockMovementDto(
        Long id, Long ingredientId, String ingredientName, String type, BigDecimal qty, String reason, String createdAt) {
}
