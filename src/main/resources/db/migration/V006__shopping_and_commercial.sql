ALTER TABLE shopping_lists ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT 'Lista de compras';
ALTER TABLE shopping_lists ADD COLUMN supplier VARCHAR(255);
ALTER TABLE shopping_lists ADD COLUMN completed_at TIMESTAMP;
ALTER TABLE shopping_lists ADD COLUMN total NUMERIC(12,2) CHECK(total >= 0);
ALTER TABLE shopping_list_products ADD COLUMN purchased_unit_price NUMERIC(10,2) CHECK(purchased_unit_price >= 0);
UPDATE shopping_list_products SET status='PENDING' WHERE status IS NULL;
ALTER TABLE shopping_list_products ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE shopping_list_products ALTER COLUMN status SET NOT NULL;
ALTER TABLE shopping_list_products ADD CONSTRAINT chk_shopping_quantity CHECK(quantity > 0) NOT VALID;
CREATE INDEX idx_shopping_lists_stock_status ON shopping_lists(stock_id, status, created_at DESC);
CREATE INDEX idx_shopping_pending ON shopping_list_products(list_id) WHERE status='PENDING';
CREATE TABLE business_expenses (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 group_id UUID NOT NULL REFERENCES groups(id) ON DELETE RESTRICT,
 created_by UUID NOT NULL REFERENCES users(id),
 description VARCHAR(255) NOT NULL,
 category VARCHAR(120) NOT NULL,
 amount NUMERIC(12,2) NOT NULL CHECK(amount > 0),
 expense_date DATE NOT NULL,
 supplier VARCHAR(255),
 shopping_list_id UUID UNIQUE REFERENCES shopping_lists(id) ON DELETE RESTRICT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_business_expenses_group_date ON business_expenses(group_id, expense_date);
-- The free checkout produces a zero-valued transaction.
ALTER TABLE transactions DROP CONSTRAINT transactions_amount_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_amount_check CHECK(amount >= 0);
