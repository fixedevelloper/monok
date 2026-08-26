ALTER TABLE coupons
    ADD COLUMN min_amount DECIMAL(12, 2) NULL AFTER amount,
    ADD COLUMN max_uses   INT           NULL AFTER expires_at,
    ADD COLUMN times_used INT NOT NULL DEFAULT 0 AFTER max_uses;

ALTER TABLE orders
    ADD COLUMN coupon_id   BIGINT UNSIGNED NULL AFTER discount,
    ADD COLUMN coupon_code VARCHAR(255)    NULL AFTER coupon_id,
    ADD CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE SET NULL;
