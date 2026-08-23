package com.monokek.ordering.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Deleting a round is a manager-approved override, same as {@link CancelOrderRequest} — see {@code OrderService#voidRound}. */
public record VoidRoundRequest(
        @NotBlank String reason,
        @NotBlank @Pattern(regexp = "\\d{4}", message = "Le code PIN doit contenir exactement 4 chiffres.") String managerPin) {
}
