package com.monokek.identity.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Raised by the {@code User} aggregate when a new staff member is registered. */
public record StaffCreatedEvent(Long userId, UUID userUuid, String name, String role, Instant occurredAt) {

    public StaffCreatedEvent(Long userId, UUID userUuid, String name, String role) {
        this(userId, userUuid, name, role, Instant.now());
    }
}
