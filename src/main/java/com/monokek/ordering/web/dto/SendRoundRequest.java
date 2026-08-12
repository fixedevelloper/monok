package com.monokek.ordering.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SendRoundRequest(
        Long orderId,
        @NotNull Long tableId,
        @NotEmpty @Valid List<ItemLine> items,
        String note
) {
    public record ItemLine(
            @NotNull Long productId,
            @NotNull @Min(1) Integer qty,
            List<ModifierLine> modifiers
    ) {
    }

    public record ModifierLine(
            @NotNull Long modifierItemId,
            @Min(1) Integer quantity
    ) {
    }
}
