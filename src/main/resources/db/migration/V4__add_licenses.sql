-- =============================================================================
-- V4__add_licenses.sql
-- Offline-verifiable signed license keys (com.monokek.licensing)
-- =============================================================================
CREATE TABLE licenses (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    token        TEXT     NOT NULL,
    activated_at DATETIME NOT NULL,
    created_at   DATETIME NULL,
    updated_at   DATETIME NULL
) ENGINE = InnoDB;
