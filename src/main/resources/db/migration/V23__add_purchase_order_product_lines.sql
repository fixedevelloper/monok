ALTER TABLE purchase_order_items
    MODIFY COLUMN ingredient_id BIGINT UNSIGNED NULL,
    ADD COLUMN product_id BIGINT UNSIGNED NULL AFTER ingredient_id,
    ADD CONSTRAINT fk_purchase_order_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_purchase_order_items_subject CHECK (
        (ingredient_id IS NOT NULL AND product_id IS NULL) OR (ingredient_id IS NULL AND product_id IS NOT NULL)
    );
