package com.monokek.ordering.web.dto;

import java.math.BigDecimal;

public record CommissionStatsDto(BigDecimal totalPending, BigDecimal totalPaid, Long topWaiterId, String topWaiterName) {
}
