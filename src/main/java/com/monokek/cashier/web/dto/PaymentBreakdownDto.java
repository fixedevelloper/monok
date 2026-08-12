package com.monokek.cashier.web.dto;

import java.math.BigDecimal;

public record PaymentBreakdownDto(String method, BigDecimal total) {
}
