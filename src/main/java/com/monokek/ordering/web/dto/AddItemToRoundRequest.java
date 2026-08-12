package com.monokek.ordering.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddItemToRoundRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer qty,
        List<SendRoundRequest.ModifierLine> modifiers
) {
}
