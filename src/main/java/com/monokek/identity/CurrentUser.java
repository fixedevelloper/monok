package com.monokek.identity;

/**
 * Published interface: the only thing other modules should know about
 * "who's making this request" — the actual user record lives in
 * monokek-identity now, not this app. Bind it directly with
 * {@code @AuthenticationPrincipal CurrentUser principal} in any module's
 * controller; Spring resolves it against the real runtime principal
 * (identity.infrastructure.security.JwtCurrentUser, built straight from the
 * bearer JWT's claims — see JwtToCurrentUserConverter) because that class
 * implements this interface.
 */
public interface CurrentUser {

    Long id();

    String name();

    /** Null means no assigned branch (e.g. an owner/super-admin) — callers that scope
     * data per branch should treat that as "unscoped: see everything". */
    Long branchId();
}
