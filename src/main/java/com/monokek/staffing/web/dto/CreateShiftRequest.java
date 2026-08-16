package com.monokek.staffing.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateShiftRequest(
        @NotNull Long userId,
        Long branchId,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        String note
) {
}
