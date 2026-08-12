package com.monokek.identity;

/**
 * Published interface: the only thing other modules should know about
 * "who's making this request" — never the full {@code identity.domain.User}
 * aggregate. Bind it directly with {@code @AuthenticationPrincipal CurrentUser
 * principal} in any module's controller; Spring resolves it against the real
 * runtime principal (identity.infrastructure.security.AuthenticatedUser)
 * because that class implements this interface.
 */
public interface CurrentUser {

    Long id();

    String name();

    /** Null means no assigned branch (e.g. an owner/super-admin) — callers that scope
     * data per branch should treat that as "unscoped: see everything". */
    Long branchId();
}
