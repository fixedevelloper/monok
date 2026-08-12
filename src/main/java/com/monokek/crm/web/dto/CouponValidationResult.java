package com.monokek.crm.web.dto;

import java.math.BigDecimal;

public record CouponValidationResult(boolean valid, BigDecimal amount, String message) {
}
