-- One-time backfill: branches created before DefaultTableProvisioner existed
-- have no fallback table for walk-in customers when the floor is full.
-- New branches get one automatically via BranchCreatedEvent.

INSERT INTO floors (branch_id, name, created_at, updated_at)
SELECT b.id, CONCAT('Emporter — ', b.name), NOW(), NOW()
FROM branches b
WHERE NOT EXISTS (
    SELECT 1 FROM floors f WHERE f.branch_id = b.id AND f.name = CONCAT('Emporter — ', b.name)
);

INSERT INTO restaurant_tables (floor_id, name, seats, status, created_at, updated_at)
SELECT f.id, f.name, 1, 'free', NOW(), NOW()
FROM floors f
         JOIN branches b ON b.id = f.branch_id
WHERE f.name = CONCAT('Emporter — ', b.name)
  AND NOT EXISTS (
    SELECT 1 FROM restaurant_tables rt WHERE rt.floor_id = f.id AND rt.name = f.name
);
