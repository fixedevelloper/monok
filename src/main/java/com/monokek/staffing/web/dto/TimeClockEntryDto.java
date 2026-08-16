package com.monokek.staffing.web.dto;

public record TimeClockEntryDto(
        Long id, Long userId, String userName, Long branchId, Long shiftId,
        String clockInAt, String clockOutAt, long workedMinutes
) {
}
