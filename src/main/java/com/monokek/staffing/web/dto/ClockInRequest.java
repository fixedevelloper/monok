package com.monokek.staffing.web.dto;

import jakarta.validation.constraints.NotNull;

/** {@code userId} is already resolved client-side via monokek-identity's PIN lookup — see the module's package-info. */
public record ClockInRequest(@NotNull Long userId, @NotNull Long branchId) {
}
