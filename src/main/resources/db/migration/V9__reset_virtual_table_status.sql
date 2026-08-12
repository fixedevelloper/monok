-- A virtual table could have been left "occupied"/"billing" by orders taken
-- before OrderService stopped marking it occupied (see V8). It must always
-- read as free.
UPDATE restaurant_tables
SET status = 'free'
WHERE is_virtual = 1
  AND status <> 'free';
