-- Marks a table as "virtual": no single customer owns it, so ordering must
-- never treat it as occupied or reuse a prior order for it (see
-- com.monokek.ordering.application.OrderService).

ALTER TABLE restaurant_tables
    ADD COLUMN is_virtual TINYINT(1) NOT NULL DEFAULT 0;

-- Backfill: mark the fallback tables DefaultTableProvisioner / V7 already created.
UPDATE restaurant_tables rt
    JOIN floors f ON f.id = rt.floor_id
    SET rt.is_virtual = 1
WHERE rt.name = f.name
  AND f.name LIKE 'Emporter — %';
