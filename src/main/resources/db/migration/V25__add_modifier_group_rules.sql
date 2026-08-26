ALTER TABLE modifiers
    ADD COLUMN type       VARCHAR(20) NOT NULL DEFAULT 'supplement' AFTER name,
    ADD COLUMN required   TINYINT(1)  NOT NULL DEFAULT 0 AFTER type,
    ADD COLUMN min_select INT         NOT NULL DEFAULT 0 AFTER required,
    ADD COLUMN max_select INT         NULL AFTER min_select,
    ADD CONSTRAINT chk_modifiers_type CHECK (type IN ('accompaniment', 'supplement'));
