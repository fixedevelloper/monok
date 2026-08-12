package com.monokek.identity.web.dto;

/** Mirrors AuthController::login's {@code {user: {id, name, role}, token}} payload. */
public record LoginResponse(UserSummary user, String token) {
}
