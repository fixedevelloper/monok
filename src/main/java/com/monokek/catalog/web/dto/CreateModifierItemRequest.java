package com.monokek.catalog.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateModifierItemRequest(@NotBlank String name, @NotNull @DecimalMin("0") BigDecimal price) {
}
