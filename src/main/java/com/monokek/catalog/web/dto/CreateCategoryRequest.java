package com.monokek.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** New functionality — see the module's package-info for why Laravel had no route to create a category directly. */
public record CreateCategoryRequest(@NotNull Long branchId, @NotBlank String name, String description, String icon, Long kitchenStationId) {
}
