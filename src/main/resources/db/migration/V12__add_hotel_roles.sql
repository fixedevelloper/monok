-- =============================================================================
-- V12__add_hotel_roles.sql
-- monokek-spring is the shared identity service for both the POS app
-- (mono-kep-tauri) and the hotel PMS (pms-frontend/pms-modulith) — see
-- SecurityConfig's CORS allow-list and pms-modulith's JwtDecoder (same HMAC
-- secret). V2__seed_rbac.sql only ever seeded restaurant roles/permissions
-- (admin/manager/cashier/waiter/kitchen); hotel staff (receptionist,
-- housekeeper) had no role to be assigned here at all, so pms-frontend's
-- permission editor and PMS staff logins had nothing real to work with.
-- =============================================================================

INSERT INTO roles (name, guard_name, created_at, updated_at) VALUES
    ('receptionist', 'api', NOW(), NOW()),
    ('housekeeper', 'api', NOW(), NOW());

-- Permissions actually referenced by pms-frontend (hasPermission/CanAccess)
-- and enforced by pms-modulith (@PreAuthorize) — see SettingController and
-- RoomController. Kept to exactly what's wired up on either side, not a
-- speculative superset.
INSERT INTO permissions (name, guard_name, created_at, updated_at) VALUES
    ('settings.view', 'api', NOW(), NOW()),
    ('settings.update', 'api', NOW(), NOW()),
    ('create_bookings', 'api', NOW(), NOW()),
    ('create_rooms', 'api', NOW(), NOW()),
    ('manage rooms', 'api', NOW(), NOW()),
    ('manage housekeeping', 'api', NOW(), NOW());

-- Demo accounts, same pattern and password ("password") as V2__seed_rbac.sql's
-- restaurant users, so the PMS permission editor has real staff to test against.
INSERT INTO users (uuid, name, phone, email, password, is_active, created_at, updated_at) VALUES
    (UUID(), 'Reception User',    '400000000', 'reception@hotel.com',    '$2y$10$5371dZTvyF2eHlgHB8KQWuXoOPYxo./14hw54wNJUNskhDsZDy3TO', 1, NOW(), NOW()),
    (UUID(), 'Housekeeping User', '300000000', 'housekeeping@hotel.com', '$2y$10$5371dZTvyF2eHlgHB8KQWuXoOPYxo./14hw54wNJUNskhDsZDy3TO', 1, NOW(), NOW());

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'receptionist' WHERE u.email = 'reception@hotel.com';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'housekeeper' WHERE u.email = 'housekeeping@hotel.com';
