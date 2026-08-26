package com.monokek.inventory.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreatePurchaseOrderRequest(@NotNull Long supplierId, @NotEmpty @Valid List<Line> items) {

    /** Exactly one of {@code ingredientId}/{@code productId} must be set — validated in {@code
     * PurchaseOrderService#store}, not here, since "exactly one of two optional fields" isn't a
     * single-field Bean Validation constraint. */
    public record Line(Long ingredientId, Long productId, @NotNull @DecimalMin("0.001") BigDecimal qty, @NotNull @DecimalMin("0") BigDecimal price) {
    }
}
