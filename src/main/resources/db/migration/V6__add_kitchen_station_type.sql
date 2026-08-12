-- New functionality: kitchen_stations.type classifies what a station actually prepares
-- (kitchen/bar/grill/pastry/other), so screens like the POS's "Comptoir Bar" can find
-- "the bar station" by type instead of a hardcoded id or a substring match on its name.
ALTER TABLE kitchen_stations
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'KITCHEN';

ALTER TABLE kitchen_stations
    ADD CONSTRAINT chk_kitchen_stations_type CHECK (type IN ('KITCHEN', 'BAR', 'GRILL', 'PASTRY', 'OTHER'));
