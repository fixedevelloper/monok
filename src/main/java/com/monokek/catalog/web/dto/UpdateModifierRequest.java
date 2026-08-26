package com.monokek.catalog.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** {@code type}/{@code required}/{@code minSelect}/{@code maxSelect} are all optional — null/absent
 * leaves that field unchanged, matching {@code name}'s own "always required" but otherwise
 * "sometimes" update semantics used everywhere else in this codebase. */
public record UpdateModifierRequest(
        @NotBlank String name, String type, Boolean required, @Min(0) Integer minSelect, @Min(0) Integer maxSelect) {
}
