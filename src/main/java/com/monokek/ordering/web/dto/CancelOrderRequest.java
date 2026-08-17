package com.monokek.ordering.web.dto;

import jakarta.validation.constraints.NotBlank;

/** A reason is mandatory — this is an audit trail entry, not a silent state flip. */
public record CancelOrderRequest(@NotBlank String reason) {
}
