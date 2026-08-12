package com.monokek.catalog.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** {@code branchId} is required here, unlike Laravel's hardcoded {@code Branch::first()} — see the module's package-info. */
public record BulkImportRequest(@NotNull Long branchId, @NotEmpty @Valid List<Line> items) {

    public record Line(@NotBlank String name, @NotNull BigDecimal price, @NotBlank String category) {
    }
}
