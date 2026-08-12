package com.monokek.ordering.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** {@code roomNumber} is only required when {@code paymentMethod} is {@code room_charge} — validated in {@code OrderService}, not here. */
public record FinalizePaymentRequest(@NotBlank String paymentMethod, @NotNull BigDecimal amountReceived, String roomNumber) {
}
