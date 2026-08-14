package com.monokek.identity.infrastructure.security;

import com.monokek.identity.CurrentUser;

/**
 * {@link CurrentUser} sourced entirely from the JWT's claims — no local {@code User}
 * table to join against anymore (see JwtToCurrentUserConverter). Record component
 * accessors satisfy the interface's {@code id()}/{@code name()}/{@code branchId()}
 * methods directly.
 */
public record JwtCurrentUser(Long id, String name, Long branchId) implements CurrentUser {
}
