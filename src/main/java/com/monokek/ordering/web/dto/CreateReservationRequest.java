package com.monokek.ordering.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public record CreateReservationRequest(
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        @NotNull @Future LocalDateTime pickupDate,
        Integer guestsCount,
        String managerNotes,
        @NotEmpty @Valid List<ItemLine> items
) {
    public record ItemLine(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {
    }
}
