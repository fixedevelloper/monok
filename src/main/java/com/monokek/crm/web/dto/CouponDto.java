package com.monokek.crm.web.dto;

import java.math.BigDecimal;

public record CouponDto(
        Long id, String code, BigDecimal amount, BigDecimal minAmount, String expiresAt,
        Integer maxUses, int timesUsed, boolean expired) {
}
