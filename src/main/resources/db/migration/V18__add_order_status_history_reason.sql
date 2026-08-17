-- Lets a cancellation (or any future status change) record why — e.g. an order
-- voided by a manager because the guest left without paying. Nullable: every
-- existing/automatic status change (markPaid, kitchen round completion) has
-- no reason to give.
ALTER TABLE order_status_histories ADD COLUMN reason TEXT NULL AFTER status;
