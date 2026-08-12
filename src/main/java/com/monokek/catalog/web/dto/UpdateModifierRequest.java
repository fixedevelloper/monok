package com.monokek.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateModifierRequest(@NotBlank String name) {
}
