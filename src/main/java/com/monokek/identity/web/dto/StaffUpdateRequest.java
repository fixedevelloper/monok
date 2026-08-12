package com.monokek.identity.web.dto;

/** All fields optional, mirroring Laravel's {@code sometimes} validation rules. */
public record StaffUpdateRequest(
        String name,
        String email,
        String phone,
        Boolean isActive,
        String role,
        Long branchId,
        boolean clearBranchId
) {
}
