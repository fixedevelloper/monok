package com.monokek.inventory.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateIngredientRequest(
        @NotBlank String name,
        @NotNull Long unitId,
        @NotNull @DecimalMin("0") BigDecimal stock,
        @NotNull @DecimalMin("0") BigDecimal alertQty
) {
}
