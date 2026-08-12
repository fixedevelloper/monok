package com.monokek.cashier.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OpenSessionRequest(@NotNull Long registerId, @NotNull @DecimalMin("0") BigDecimal openingAmount) {
}
