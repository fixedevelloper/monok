package com.monokek.identity.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Published by {@code AuthService#login} on a successful login. */
public record UserLoggedInEvent(Long userId, UUID userUuid, String deviceName, Instant occurredAt) {

    public UserLoggedInEvent(Long userId, UUID userUuid, String deviceName) {
        this(userId, userUuid, deviceName, Instant.now());
    }
}
