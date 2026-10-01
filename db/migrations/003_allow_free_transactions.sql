-- FREE checkout is already supported by TransactionService and records amount=0.
-- Keep negative amounts forbidden while allowing existing databases to use it.
BEGIN;

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_amount_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_amount_check CHECK (amount >= 0);

COMMIT;
