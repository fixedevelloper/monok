package com.monokek.inventory.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateIngredientRequest(
        @NotBlank String name,
        @NotNull @JsonAlias("unit_id") Long unitId,
        @NotNull @DecimalMin("0") BigDecimal stock,
        @NotNull @JsonAlias("alert_qty") @DecimalMin("0") BigDecimal alertQty
) {
}
