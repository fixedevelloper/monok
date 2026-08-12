package com.monokek.ordering.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinalizePaymentRequest(@NotBlank String paymentMethod, @NotNull BigDecimal amountReceived) {
}
