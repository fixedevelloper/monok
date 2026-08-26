package com.monokek.crm.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCouponRequest(
        @NotBlank String code,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @DecimalMin("0") BigDecimal minAmount,
        LocalDateTime expiresAt,
        @Min(1) Integer maxUses
) {
}
