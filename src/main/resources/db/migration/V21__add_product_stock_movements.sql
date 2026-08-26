CREATE TABLE product_stock_movements (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT UNSIGNED NOT NULL,
    type       VARCHAR(10) NOT NULL, -- in, out, adjust
    qty        INT NOT NULL,         -- signed change in stock_count (negative for "out")
    reason     TEXT NULL,
    author_id  BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_product_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_stock_movements_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;
