package com.monokek.reporting.web.dto;

import java.math.BigDecimal;

public record PaymentMethodTotal(String method, long count, BigDecimal total) {
}
