-- Laravel's OrderController::notifyKitchenOfQuantityIncrease() tries to persist a
-- 'note' on kitchen_tickets (e.g. "Supplément : +2 Frites") that the original schema
-- never defined — inserting it would fail at runtime. Adding the column for real,
-- since the intent (telling kitchen staff about a supplemental item) is clearly useful.
ALTER TABLE kitchen_tickets
    ADD COLUMN note TEXT NULL AFTER station_id;
