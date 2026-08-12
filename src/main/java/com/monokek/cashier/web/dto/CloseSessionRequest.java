package com.monokek.cashier.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CloseSessionRequest(@NotNull @DecimalMin("0") BigDecimal closingAmount, String note) {
}
