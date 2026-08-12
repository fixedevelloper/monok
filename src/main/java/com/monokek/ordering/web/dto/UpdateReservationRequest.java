package com.monokek.ordering.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateReservationRequest(
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        @NotNull LocalDateTime pickupDate,
        Integer guestsCount,
        String managerNotes,
        @NotEmpty @Valid List<CreateReservationRequest.ItemLine> items
) {
}
