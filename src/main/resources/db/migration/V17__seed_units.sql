-- =============================================================================
-- V17__seed_units.sql
-- 'units' (kg/L/pcs, per its own column comment in V1__init_schema.sql) was
-- created empty and never seeded — AddIngredientModal's unit dropdown reads
-- straight from GET /api/admin/units (IngredientController#units), so with
-- no rows there's nothing to pick and no way to create an ingredient at all
-- (ingredients.unit_id is NOT NULL).
-- =============================================================================

INSERT INTO units (name) VALUES
    ('kg'),
    ('g'),
    ('L'),
    ('ml'),
    ('pièce');
