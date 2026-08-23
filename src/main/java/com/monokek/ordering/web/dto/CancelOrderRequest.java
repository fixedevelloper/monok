package com.monokek.ordering.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * A reason is mandatory — this is an audit trail entry, not a silent state flip. {@code managerPin}
 * is mandatory too: cancelling an order is always a manager-approved override now, whether or not
 * the caller's own session already holds {@code cancel_orders} — see {@code OrderService#cancelOrder}.
 */
public record CancelOrderRequest(
        @NotBlank String reason,
        @NotBlank @Pattern(regexp = "\\d{4}", message = "Le code PIN doit contenir exactement 4 chiffres.") String managerPin) {
}
