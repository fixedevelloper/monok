package com.monokek.staffing.web.dto;

import java.time.LocalDateTime;

/** All fields optional — only the ones present are applied. */
public record UpdateShiftRequest(
        Long userId, Long branchId, LocalDateTime startsAt, LocalDateTime endsAt, String note
) {
}
