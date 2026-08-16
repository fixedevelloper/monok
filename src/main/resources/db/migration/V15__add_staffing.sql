CREATE TABLE shifts (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED NOT NULL,
    branch_id  BIGINT UNSIGNED NOT NULL,
    starts_at  DATETIME NOT NULL,
    ends_at    DATETIME NOT NULL,
    note       VARCHAR(255) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_shifts_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE,
    CONSTRAINT fk_shifts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_shifts_branch_starts ON shifts (branch_id, starts_at);
CREATE INDEX idx_shifts_user_starts ON shifts (user_id, starts_at);

CREATE TABLE time_clock_entries (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT UNSIGNED NOT NULL,
    branch_id    BIGINT UNSIGNED NOT NULL,
    shift_id     BIGINT UNSIGNED NULL,
    clock_in_at  DATETIME NOT NULL,
    clock_out_at DATETIME NULL,
    created_at   DATETIME NULL,
    updated_at   DATETIME NULL,
    CONSTRAINT fk_time_clock_entries_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE,
    CONSTRAINT fk_time_clock_entries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_time_clock_entries_shift FOREIGN KEY (shift_id) REFERENCES shifts (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE INDEX idx_tce_branch_open ON time_clock_entries (branch_id, clock_out_at);
CREATE INDEX idx_tce_user_open ON time_clock_entries (user_id, clock_out_at);
