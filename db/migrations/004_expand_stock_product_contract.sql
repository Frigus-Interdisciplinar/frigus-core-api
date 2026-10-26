-- Expand product and stock-product metadata without invalidating existing rows.
-- Safe to reapply; rollback is the previous application version with these columns left in place.

BEGIN;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS brand VARCHAR(120),
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(2048);

ALTER TABLE stock_products
    ADD COLUMN IF NOT EXISTS purchase_unit_price NUMERIC(10, 2);

ALTER TABLE stock_products
    DROP CONSTRAINT IF EXISTS chk_stock_products_purchase_unit_price;

ALTER TABLE stock_products
    ADD CONSTRAINT chk_stock_products_purchase_unit_price
    CHECK (purchase_unit_price IS NULL OR purchase_unit_price > 0);

COMMIT;
