package com.monokek.catalog.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code type} is "in"/"out"/"adjust". For "in"/"out", {@code qty} is the quantity moved
 * (always positive). For "adjust", {@code qty} is the new absolute stock count after a physical
 * count — the service computes and records the resulting delta.
 */
public record AdjustProductStockRequest(
        @NotBlank String type,
        @NotNull @Min(0) Integer qty,
        String reason
) {
}
