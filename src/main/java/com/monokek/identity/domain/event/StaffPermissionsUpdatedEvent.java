package com.monokek.identity.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Raised when a staff member's direct permission grants are replaced (see StaffController::updatePermissions). */
public record StaffPermissionsUpdatedEvent(Long userId, UUID userUuid, List<String> permissions, Instant occurredAt) {

    public StaffPermissionsUpdatedEvent(Long userId, UUID userUuid, List<String> permissions) {
        this(userId, userUuid, permissions, Instant.now());
    }
}
