package com.monokek.staffing.web.dto;

public record ShiftDto(
        Long id, Long userId, String userName, Long branchId, String startsAt, String endsAt, String note
) {
}
