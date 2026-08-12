package com.monokek.inventory.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustStockRequest(@NotNull @DecimalMin("0.001") BigDecimal qty, @NotBlank String type, String reason) {
}
