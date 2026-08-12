-- =============================================================================
-- V2__seed_rbac.sql
-- Mirrors database/seeders/RoleSeeder.php, PermissionSeeder.php,
-- AdminUserSeeder.php and PaymentMethodSeeder.php from the Laravel app.
-- =============================================================================

INSERT INTO roles (name, guard_name, created_at, updated_at) VALUES
    ('admin', 'api', NOW(), NOW()),
    ('manager', 'api', NOW(), NOW()),
    ('cashier', 'api', NOW(), NOW()),
    ('waiter', 'api', NOW(), NOW()),
    ('kitchen', 'api', NOW(), NOW());

INSERT INTO permissions (name, guard_name, created_at, updated_at) VALUES
    -- users
    ('manage_users', 'api', NOW(), NOW()),
    ('view_staff', 'api', NOW(), NOW()),
    ('edit_permissions', 'api', NOW(), NOW()),
    -- orders
    ('create_orders', 'api', NOW(), NOW()),
    ('edit_orders', 'api', NOW(), NOW()),
    ('cancel_orders', 'api', NOW(), NOW()),
    ('view_history', 'api', NOW(), NOW()),
    -- products
    ('manage_products', 'api', NOW(), NOW()),
    ('manage_stock', 'api', NOW(), NOW()),
    ('manage_categories', 'api', NOW(), NOW()),
    -- reports
    ('view_reports', 'api', NOW(), NOW()),
    ('view_analytics', 'api', NOW(), NOW()),
    -- finance
    ('close_cashier', 'api', NOW(), NOW()),
    ('manage_discounts', 'api', NOW(), NOW()),
    -- settings
    ('manage_settings', 'api', NOW(), NOW()),
    ('manage_branch', 'api', NOW(), NOW());

INSERT INTO payment_methods (name, created_at, updated_at) VALUES
    ('cash', NOW(), NOW()),
    ('momo', NOW(), NOW()),
    ('card', NOW(), NOW());

-- Password for every seeded user is "password" (bcrypt hash below), exactly
-- like AdminUserSeeder.php — change it immediately outside of local/dev use.
INSERT INTO users (uuid, name, phone, email, password, is_active, created_at, updated_at) VALUES
    (UUID(), 'Super Admin',  '000000000', 'admin@resto.com',   '$2y$10$5371dZTvyF2eHlgHB8KQWuXoOPYxo./14hw54wNJUNskhDsZDy3TO', 1, NOW(), NOW()),
    (UUID(), 'Cashier User', '700000000', 'cashier@resto.com', '$2y$10$5371dZTvyF2eHlgHB8KQWuXoOPYxo./14hw54wNJUNskhDsZDy3TO', 1, NOW(), NOW()),
    (UUID(), 'Kitchen User', '600000000', 'kitchen@resto.com', '$2y$10$5371dZTvyF2eHlgHB8KQWuXoOPYxo./14hw54wNJUNskhDsZDy3TO', 1, NOW(), NOW()),
    (UUID(), 'Waiter User',  '500000000', 'waiter@resto.com',  '$2y$10$5371dZTvyF2eHlgHB8KQWuXoOPYxo./14hw54wNJUNskhDsZDy3TO', 1, NOW(), NOW());

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'admin'   WHERE u.email = 'admin@resto.com';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'cashier' WHERE u.email = 'cashier@resto.com';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'kitchen' WHERE u.email = 'kitchen@resto.com';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'waiter'  WHERE u.email = 'waiter@resto.com';
