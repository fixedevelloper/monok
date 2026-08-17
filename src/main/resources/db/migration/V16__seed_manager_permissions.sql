-- =============================================================================
-- V16__seed_manager_permissions.sql
-- Until now role_permissions was empty for every role — permissions only ever
-- existed as per-user direct grants (user_permissions, via PermissionsModal).
-- This gives the 'manager' role a sensible operational default so enforcing
-- @PreAuthorize(hasAuthority(...)) on top of the existing role checks doesn't
-- lock every manager out on day one. 'admin' needs no row here: it bypasses
-- permission checks in code (hasRole('ADMIN') or hasAuthority(...)), by design.
--
-- Deliberately excluded for manager (admin-only): manage_users,
-- edit_permissions, manage_branch, manage_settings.
-- =============================================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'view_staff',
    'create_orders', 'edit_orders', 'cancel_orders', 'view_history',
    'manage_products', 'manage_stock', 'manage_categories',
    'view_reports', 'view_analytics',
    'close_cashier', 'manage_discounts'
)
WHERE r.name = 'manager';
