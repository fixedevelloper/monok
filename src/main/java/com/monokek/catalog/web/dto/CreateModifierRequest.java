package com.monokek.catalog.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateModifierRequest(@NotBlank String name, @Valid List<Item> items) {

    public record Item(@NotBlank String name, @NotNull @DecimalMin("0") BigDecimal price) {
    }
}
