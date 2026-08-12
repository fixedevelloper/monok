package com.monokek.inventory.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record RecipeRequest(@NotEmpty @Valid List<Line> items) {

    public record Line(@NotNull Long ingredientId, @NotNull @DecimalMin("0.0001") BigDecimal qty) {
    }
}
