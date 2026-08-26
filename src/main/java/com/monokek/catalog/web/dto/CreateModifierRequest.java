package com.monokek.catalog.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** {@code type}/{@code required}/{@code minSelect}/{@code maxSelect} are all optional — null/absent
 * falls back to a plain optional "supplement" group (0 minimum, no maximum), matching every group
 * created before this feature existed. */
public record CreateModifierRequest(
        @NotBlank String name, String type, Boolean required, @Min(0) Integer minSelect, @Min(0) Integer maxSelect,
        @Valid List<Item> items) {

    public record Item(@NotBlank String name, @NotNull @DecimalMin("0") BigDecimal price) {
    }
}
