-- "Chambre" as a payment method: bills the order to a hotel guest's room
-- folio in pms-modulith instead of collecting cash/momo/card at the till.
-- See the `pms` module (OrderService.finalizePayment routes this method
-- through PmsClient before recording the till payment).
INSERT INTO payment_methods (name, created_at, updated_at) VALUES ('room_charge', NOW(), NOW());
