
-- =============================================================================
-- V1__init_schema.sql
-- Spring Modulith port of MonoKek (Laravel) — database/migrations/*.php
-- Restaurant POS + Kitchen + Stock + RBAC + LAN printing
-- =============================================================================

-- -----------------------------------------------------------------------------
-- RBAC (replaces spatie/laravel-permission: roles, permissions, role_has_permissions,
-- model_has_roles, model_has_permissions — simplified since `User` is the only
-- model carrying roles/permissions in this app).
-- -----------------------------------------------------------------------------
CREATE TABLE roles (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    guard_name VARCHAR(255) NOT NULL DEFAULT 'api',
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    UNIQUE KEY uq_roles_name_guard (name, guard_name)
) ENGINE = InnoDB;

CREATE TABLE permissions (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    guard_name VARCHAR(255) NOT NULL DEFAULT 'api',
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    UNIQUE KEY uq_permissions_name_guard (name, guard_name)
) ENGINE = InnoDB;

CREATE TABLE role_permissions (
    role_id       BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- USERS / SECURITY
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid       CHAR(36)     NOT NULL,
    name       VARCHAR(255) NOT NULL,
    phone      VARCHAR(255) NULL,
    pin_code   VARCHAR(255) NULL,
    email      VARCHAR(255) NULL,
    password   VARCHAR(255) NOT NULL,
    is_active  TINYINT(1)   NOT NULL DEFAULT 1,
    remember_token VARCHAR(100) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    deleted_at DATETIME NULL,
    UNIQUE KEY uq_users_uuid (uuid),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Direct (extra) permissions granted to a user, on top of what their role(s) grant.
CREATE TABLE user_permissions (
    user_id       BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE devices (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NULL,
    device_name VARCHAR(255) NOT NULL,
    device_type VARCHAR(255) NOT NULL, -- mobile,pos,kitchen
    device_uuid VARCHAR(255) NOT NULL,
    ip_address  VARCHAR(255) NULL,
    created_at  DATETIME NULL,
    updated_at  DATETIME NULL,
    UNIQUE KEY uq_devices_uuid (device_uuid),
    CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- BUSINESS / BRANCHES
-- -----------------------------------------------------------------------------
CREATE TABLE companies (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    phone      VARCHAR(255) NULL,
    email      VARCHAR(255) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL
) ENGINE = InnoDB;

CREATE TABLE branches (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT UNSIGNED NOT NULL,
    name       VARCHAR(255) NOT NULL,
    address    VARCHAR(255) NULL,
    phone      VARCHAR(255) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_branches_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE workstations (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id  BIGINT UNSIGNED NOT NULL,
    name       VARCHAR(255) NOT NULL, -- caisse1 cuisine1
    type       VARCHAR(255) NOT NULL, -- pos kitchen admin
    ip         VARCHAR(255) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_workstations_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE printers (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id      BIGINT UNSIGNED NOT NULL,
    name           VARCHAR(255) NOT NULL,
    type           VARCHAR(20)  NOT NULL DEFAULT 'escpos',
    connection     VARCHAR(20)  NOT NULL DEFAULT 'network',
    location       VARCHAR(255) NOT NULL DEFAULT 'receipt',
    ip             VARCHAR(255) NULL,
    port           INT NOT NULL DEFAULT 9100,
    char_per_line  INT NOT NULL DEFAULT 42,
    is_active      TINYINT(1) NOT NULL DEFAULT 1,
    paper_width    INT NOT NULL DEFAULT 58,
    use_beep       TINYINT(1) NOT NULL DEFAULT 1,
    created_at     DATETIME NULL,
    updated_at     DATETIME NULL,
    CONSTRAINT fk_printers_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE,
    CONSTRAINT chk_printers_type CHECK (type IN ('escpos', 'label', 'pdf')),
    CONSTRAINT chk_printers_connection CHECK (connection IN ('usb', 'network', 'bt'))
) ENGINE = InnoDB;

CREATE TABLE print_queues (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    printer_id    BIGINT UNSIGNED NOT NULL,
    job_type      VARCHAR(255) NOT NULL, -- order, kitchen, bill
    content       JSON NOT NULL,
    attempts      INT NOT NULL DEFAULT 0,
    status        VARCHAR(20) NOT NULL DEFAULT 'pending',
    error_message TEXT NULL,
    priority      TINYINT UNSIGNED NOT NULL DEFAULT 1,
    printed_at    DATETIME NULL,
    created_at    DATETIME NULL,
    updated_at    DATETIME NULL,
    CONSTRAINT fk_print_queues_printer FOREIGN KEY (printer_id) REFERENCES printers (id) ON DELETE CASCADE,
    CONSTRAINT chk_print_queues_status CHECK (status IN ('pending', 'printing', 'completed', 'failed'))
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- FLOOR / TABLES
-- -----------------------------------------------------------------------------
CREATE TABLE floors (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id  BIGINT UNSIGNED NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_floors_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE restaurant_tables (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    floor_id   BIGINT UNSIGNED NOT NULL,
    name       VARCHAR(255) NOT NULL,
    seats      INT NOT NULL DEFAULT 4,
    status     VARCHAR(50) NOT NULL DEFAULT 'free',
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_restaurant_tables_floor FOREIGN KEY (floor_id) REFERENCES floors (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- KITCHEN (created before `categories`, which has an FK to kitchen_stations)
-- -----------------------------------------------------------------------------
CREATE TABLE kitchen_stations (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id  BIGINT UNSIGNED NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_kitchen_stations_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- PRODUCTS
-- -----------------------------------------------------------------------------
CREATE TABLE categories (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id           BIGINT UNSIGNED NOT NULL,
    kitchen_station_id  BIGINT UNSIGNED NULL,
    name                VARCHAR(255) NOT NULL,
    slug                VARCHAR(255) NOT NULL,
    description         VARCHAR(255) NULL,
    icon                VARCHAR(255) NULL,
    is_active           TINYINT(1) NOT NULL DEFAULT 1,
    created_at          DATETIME NULL,
    updated_at          DATETIME NULL,
    UNIQUE KEY uq_categories_name (name),
    UNIQUE KEY uq_categories_slug (slug),
    CONSTRAINT fk_categories_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE,
    CONSTRAINT fk_categories_kitchen_station FOREIGN KEY (kitchen_station_id) REFERENCES kitchen_stations (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE products (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id       BIGINT UNSIGNED NOT NULL,
    sku               VARCHAR(255) NULL,
    name              VARCHAR(255) NOT NULL,
    description       TEXT NULL,
    price             DECIMAL(12, 2) NOT NULL,
    incentive_amount  DECIMAL(10, 2) NULL DEFAULT 0,
    image             VARCHAR(255) NULL,
    type              VARCHAR(20) NOT NULL DEFAULT 'storable',
    stock_count       INT NOT NULL DEFAULT 0,
    alert_stock       INT NOT NULL DEFAULT 0,
    is_active         TINYINT(1) NOT NULL DEFAULT 1,
    track_stock       TINYINT(1) NOT NULL DEFAULT 0,
    created_at        DATETIME NULL,
    updated_at        DATETIME NULL,
    deleted_at        DATETIME NULL,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT chk_products_type CHECK (type IN ('storable', 'consumable', 'service'))
) ENGINE = InnoDB;

CREATE TABLE product_variants (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT UNSIGNED NOT NULL,
    name       VARCHAR(255) NOT NULL, -- Large Medium
    price      DECIMAL(12, 2) NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE modifiers (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL, -- extras
    created_at DATETIME NULL,
    updated_at DATETIME NULL
) ENGINE = InnoDB;

CREATE TABLE modifier_items (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    modifier_id BIGINT UNSIGNED NOT NULL,
    name       VARCHAR(255) NOT NULL,
    price      DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_modifier_items_modifier FOREIGN KEY (modifier_id) REFERENCES modifiers (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE modifier_product (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    modifier_id BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  DATETIME NULL,
    updated_at  DATETIME NULL,
    UNIQUE KEY uq_modifier_product (modifier_id, product_id),
    CONSTRAINT fk_modifier_product_modifier FOREIGN KEY (modifier_id) REFERENCES modifiers (id) ON DELETE CASCADE,
    CONSTRAINT fk_modifier_product_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- CUSTOMERS
-- -----------------------------------------------------------------------------
CREATE TABLE customers (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NULL,
    phone      VARCHAR(255) NULL,
    email      VARCHAR(255) NULL,
    points     INT NOT NULL DEFAULT 0,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    KEY idx_customers_phone (phone)
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- ORDERS
-- -----------------------------------------------------------------------------
CREATE TABLE orders (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid        CHAR(36) NOT NULL,
    reference   VARCHAR(255) NOT NULL,
    branch_id   BIGINT UNSIGNED NOT NULL,
    table_id    BIGINT UNSIGNED NULL,
    customer_id BIGINT UNSIGNED NULL,
    user_id     BIGINT UNSIGNED NOT NULL, -- waiter
    cashier_id  BIGINT UNSIGNED NOT NULL,
    type        VARCHAR(50) NOT NULL DEFAULT 'dinein', -- takeaway delivery
    status      VARCHAR(30) NOT NULL DEFAULT 'draft',
    subtotal    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    tax         DECIMAL(12, 2) NOT NULL DEFAULT 0,
    discount    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    source      VARCHAR(255) NULL,
    note        TEXT NULL,
    paid_at     DATETIME NULL,
    created_at  DATETIME NULL,
    updated_at  DATETIME NULL,
    UNIQUE KEY uq_orders_uuid (uuid),
    UNIQUE KEY uq_orders_reference (reference),
    KEY idx_orders_branch_status (branch_id, status),
    CONSTRAINT fk_orders_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_table FOREIGN KEY (table_id) REFERENCES restaurant_tables (id) ON DELETE SET NULL,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE SET NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_cashier FOREIGN KEY (cashier_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_orders_status CHECK (status IN
        ('draft', 'pending_payment', 'pending', 'billing', 'reserved', 'paid', 'completed', 'cancelled', 'ready', 'preparing'))
) ENGINE = InnoDB;

CREATE TABLE order_rounds (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT UNSIGNED NOT NULL,
    round_number INT NOT NULL DEFAULT 1,
    status       VARCHAR(20) NOT NULL DEFAULT 'pending',
    note         TEXT NULL,
    sent_at      DATETIME NULL,
    created_at   DATETIME NULL,
    updated_at   DATETIME NULL,
    CONSTRAINT fk_order_rounds_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_order_rounds_status CHECK (status IN ('pending', 'sent', 'preparing', 'served'))
) ENGINE = InnoDB;

CREATE TABLE order_items (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_round_id BIGINT UNSIGNED NOT NULL,
    product_id     BIGINT UNSIGNED NOT NULL,
    variant_id     BIGINT UNSIGNED NULL,
    qty            INT NOT NULL,
    price          DECIMAL(12, 2) NOT NULL,
    total          DECIMAL(12, 2) NOT NULL,
    status         VARCHAR(30) NOT NULL DEFAULT 'pending',
    created_at     DATETIME NULL,
    updated_at     DATETIME NULL,
    CONSTRAINT fk_order_items_round FOREIGN KEY (order_round_id) REFERENCES order_rounds (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE order_item_modifiers (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_item_id     BIGINT UNSIGNED NOT NULL,
    modifier_item_id  BIGINT UNSIGNED NOT NULL,
    quantity          INT NOT NULL DEFAULT 1,
    price             DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total             DECIMAL(12, 2) AS (quantity * price) STORED,
    CONSTRAINT fk_order_item_modifiers_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_modifiers_modifier_item FOREIGN KEY (modifier_item_id) REFERENCES modifier_items (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE order_status_histories (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT UNSIGNED NOT NULL,
    status     VARCHAR(30) NOT NULL,
    user_id    BIGINT UNSIGNED NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_order_status_histories_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_histories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE reservations (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id           BIGINT UNSIGNED NOT NULL,
    customer_id        BIGINT UNSIGNED NOT NULL,
    pickup_date        DATETIME NOT NULL,
    guests_count       INT NOT NULL DEFAULT 1,
    manager_notes      TEXT NULL,
    reservation_status VARCHAR(20) NOT NULL DEFAULT 'confirmed',
    created_at         DATETIME NULL,
    updated_at         DATETIME NULL,
    CONSTRAINT fk_reservations_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT chk_reservations_status CHECK (reservation_status IN ('confirmed', 'arrived', 'no_show'))
) ENGINE = InnoDB;

CREATE TABLE commissions (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT UNSIGNED NOT NULL,
    order_id      BIGINT UNSIGNED NOT NULL,
    order_item_id BIGINT UNSIGNED NULL,
    amount        DECIMAL(10, 2) NOT NULL,
    percentage    FLOAT NULL,
    type          VARCHAR(20) NOT NULL DEFAULT 'global',
    status        VARCHAR(20) NOT NULL DEFAULT 'pending',
    paid_at       DATETIME NULL,
    created_at    DATETIME NULL,
    updated_at    DATETIME NULL,
    CONSTRAINT fk_commissions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_commissions_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_commissions_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT chk_commissions_type CHECK (type IN ('global', 'incentive')),
    CONSTRAINT chk_commissions_status CHECK (status IN ('pending', 'paid'))
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- KITCHEN (tickets, once orders exist)
-- -----------------------------------------------------------------------------
CREATE TABLE kitchen_tickets (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_round_id BIGINT UNSIGNED NOT NULL,
    order_id       BIGINT UNSIGNED NOT NULL,
    station_id     BIGINT UNSIGNED NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'pending',
    priority       INT NOT NULL DEFAULT 1,
    created_at     DATETIME NULL,
    updated_at     DATETIME NULL,
    CONSTRAINT fk_kitchen_tickets_round FOREIGN KEY (order_round_id) REFERENCES order_rounds (id) ON DELETE CASCADE,
    CONSTRAINT fk_kitchen_tickets_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_kitchen_tickets_station FOREIGN KEY (station_id) REFERENCES kitchen_stations (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- CASHIER / PAYMENTS
-- -----------------------------------------------------------------------------
CREATE TABLE cash_registers (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id  BIGINT UNSIGNED NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_cash_registers_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE cash_sessions (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    register_id     BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    opening_amount  DECIMAL(12, 2) NOT NULL,
    closing_amount  DECIMAL(12, 2) NULL,
    opened_at       DATETIME NOT NULL,
    closed_at       DATETIME NULL,
    expected_amount DECIMAL(12, 2) NULL,
    note            TEXT NULL,
    created_at      DATETIME NULL,
    updated_at      DATETIME NULL,
    CONSTRAINT fk_cash_sessions_register FOREIGN KEY (register_id) REFERENCES cash_registers (id) ON DELETE CASCADE,
    CONSTRAINT fk_cash_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE payment_methods (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL, -- cash momo card
    created_at DATETIME NULL,
    updated_at DATETIME NULL
) ENGINE = InnoDB;

CREATE TABLE payments (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id          BIGINT UNSIGNED NOT NULL,
    payment_method_id BIGINT UNSIGNED NOT NULL,
    cash_session_id   BIGINT UNSIGNED NOT NULL,
    amount            DECIMAL(12, 2) NOT NULL,
    change_due        DECIMAL(12, 2) NOT NULL,
    amount_received   DECIMAL(12, 2) NOT NULL,
    reference         VARCHAR(255) NULL,
    created_at        DATETIME NULL,
    updated_at        DATETIME NULL,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_cash_session FOREIGN KEY (cash_session_id) REFERENCES cash_sessions (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- STOCK
-- -----------------------------------------------------------------------------
CREATE TABLE units (
    id   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL -- kg l pcs
) ENGINE = InnoDB;

CREATE TABLE ingredients (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    unit_id    BIGINT UNSIGNED NOT NULL,
    name       VARCHAR(255) NOT NULL,
    stock      DECIMAL(12, 3) NOT NULL DEFAULT 0,
    alert_qty  DECIMAL(12, 3) NOT NULL DEFAULT 0,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_ingredients_unit FOREIGN KEY (unit_id) REFERENCES units (id)
) ENGINE = InnoDB;

CREATE TABLE recipes (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_recipes_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE recipe_items (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    recipe_id     BIGINT UNSIGNED NOT NULL,
    ingredient_id BIGINT UNSIGNED NOT NULL,
    qty           DECIMAL(12, 3) NOT NULL,
    CONSTRAINT fk_recipe_items_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_items_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE suppliers (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    phone      VARCHAR(255) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL
) ENGINE = InnoDB;

CREATE TABLE purchase_orders (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    supplier_id BIGINT UNSIGNED NOT NULL,
    status      VARCHAR(30) NOT NULL DEFAULT 'draft',
    total       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at  DATETIME NULL,
    updated_at  DATETIME NULL,
    CONSTRAINT fk_purchase_orders_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE purchase_order_items (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id BIGINT UNSIGNED NOT NULL,
    ingredient_id     BIGINT UNSIGNED NOT NULL,
    qty               DECIMAL(12, 3) NOT NULL,
    price             DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_purchase_order_items_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_order_items_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE stock_movements (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    ingredient_id BIGINT UNSIGNED NOT NULL,
    type          VARCHAR(10) NOT NULL, -- in,out,adjust
    qty           DECIMAL(12, 3) NOT NULL,
    reason        TEXT NULL,
    created_at    DATETIME NULL,
    updated_at    DATETIME NULL,
    CONSTRAINT fk_stock_movements_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- PROMO / CRM
-- -----------------------------------------------------------------------------
CREATE TABLE coupons (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(255) NOT NULL,
    amount     DECIMAL(12, 2) NOT NULL,
    expires_at DATETIME NULL,
    UNIQUE KEY uq_coupons_code (code)
) ENGINE = InnoDB;

CREATE TABLE loyalty_transactions (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT UNSIGNED NOT NULL,
    points      INT NOT NULL,
    type        VARCHAR(10) NOT NULL, -- earn/redeem
    created_at  DATETIME NULL,
    updated_at  DATETIME NULL,
    CONSTRAINT fk_loyalty_transactions_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- LOGS / SETTINGS
-- -----------------------------------------------------------------------------
CREATE TABLE settings (
    id    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `key` VARCHAR(255) NOT NULL,
    value TEXT NULL,
    UNIQUE KEY uq_settings_key (`key`)
) ENGINE = InnoDB;

CREATE TABLE activity_logs (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED NULL,
    action     TEXT NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE sync_logs (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_uuid VARCHAR(255) NOT NULL,
    status      VARCHAR(255) NOT NULL,
    created_at  DATETIME NULL,
    updated_at  DATETIME NULL
) ENGINE = InnoDB;
