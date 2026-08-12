package com.monokek.crm.web.dto;

import java.math.BigDecimal;

public record CouponDto(Long id, String code, BigDecimal amount, String expiresAt, boolean expired) {
}
