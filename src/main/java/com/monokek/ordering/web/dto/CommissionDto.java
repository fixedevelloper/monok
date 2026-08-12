package com.monokek.ordering.web.dto;

import java.math.BigDecimal;

/** Mirrors {@code App\Http\Resources\CommissionResource}. */
public record CommissionDto(
        Long id,
        BigDecimal amount,
        Float percentage,
        String type,
        String status,
        String createdAt,
        Long waiterId,
        String waiterName,
        String orderReference,
        String productName,
        boolean isIncentive,
        String statusLabel
) {
}
