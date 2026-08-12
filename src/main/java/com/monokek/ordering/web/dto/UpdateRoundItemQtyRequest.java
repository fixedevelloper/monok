package com.monokek.ordering.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateRoundItemQtyRequest(@NotNull @Min(0) Integer qty) {
}
