package com.monokek.licensing.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Matches the frontend's {@code { key: activationKey }} POST body. */
public record ActivateLicenseRequest(@NotBlank String key) {
}
