-- Lets a cashier (with manager PIN approval — see identity's /api/auth/lookup-pin)
-- remove a round without physically deleting it: 'voided' joins the existing
-- pending/sent/preparing/served vocabulary, and who approved + why is kept on the
-- row itself for the admin activity log to surface.
ALTER TABLE order_rounds
    DROP CHECK chk_order_rounds_status;

ALTER TABLE order_rounds
    ADD COLUMN voided_by_user_id BIGINT UNSIGNED NULL AFTER status,
    ADD COLUMN void_reason       TEXT NULL AFTER voided_by_user_id,
    ADD COLUMN voided_at         DATETIME NULL AFTER void_reason,
    ADD CONSTRAINT chk_order_rounds_status CHECK (status IN ('pending', 'sent', 'preparing', 'served', 'voided')),
    ADD CONSTRAINT fk_order_rounds_voided_by FOREIGN KEY (voided_by_user_id) REFERENCES users (id) ON DELETE SET NULL;
