package com.monokek.identity.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Raised when a staff member's access is revoked (soft delete, see StaffController::destroy). */
public record StaffAccessRevokedEvent(Long userId, UUID userUuid, Long revokedByUserId, Instant occurredAt) {

    public StaffAccessRevokedEvent(Long userId, UUID userUuid, Long revokedByUserId) {
        this(userId, userUuid, revokedByUserId, Instant.now());
    }
}
