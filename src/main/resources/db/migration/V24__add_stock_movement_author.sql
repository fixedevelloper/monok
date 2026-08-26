ALTER TABLE stock_movements
    ADD COLUMN author_id BIGINT UNSIGNED NULL AFTER reason,
    ADD CONSTRAINT fk_stock_movements_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL;
