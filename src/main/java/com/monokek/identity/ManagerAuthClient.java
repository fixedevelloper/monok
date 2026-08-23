package com.monokek.identity;

/**
 * Published interface: verifies a manager-override PIN typed at the till (a cashier voiding an
 * order/round, or any other action that needs a manager's approval without them logging in) —
 * calls monokek-identity's own {@code /api/auth/lookup-pin}, the same primitive the shared-terminal
 * clock-in flow already uses to identify a colleague by PIN. Never exposes the caller a raw
 * {@code identity.domain.User} or password/PIN hash, only who was identified and their role.
 */
public interface ManagerAuthClient {

    /**
     * Resolves {@code pin} to a staff member in {@code branchId} and asserts they hold a role
     * allowed to approve overrides (ADMIN or MANAGER). Throws {@code ApiException} (unauthorized/
     * forbidden) otherwise — never returns a non-approving result.
     */
    ManagerApproval verifyManagerPin(Long branchId, String pin, String bearerToken);

    record ManagerApproval(Long userId, String name, String role) {
    }
}
