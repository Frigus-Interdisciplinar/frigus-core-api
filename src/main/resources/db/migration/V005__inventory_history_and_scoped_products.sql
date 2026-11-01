-- Java holds the row lock and applies movements. The legacy trigger applied them a second time.
DROP TRIGGER IF EXISTS trg_stock_movements_apply ON stock_movements;
DROP FUNCTION IF EXISTS trg_fn_stock_movements_apply();
ALTER TABLE stock_movements ADD COLUMN observation VARCHAR(2000);
ALTER TABLE stock_movements ADD COLUMN balance_after INTEGER;
ALTER TABLE stock_movements ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'INVENTORY'
 CHECK (purpose IN ('INVENTORY','CONSUMPTION','DISCARD'));
UPDATE stock_movements SET purpose='CONSUMPTION' WHERE movement_type='OUT';
ALTER TABLE "discard" ADD COLUMN IF NOT EXISTS quantity INTEGER;
ALTER TABLE "discard" ALTER COLUMN quantity DROP NOT NULL;
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'chk_discard_quantity_positive'
      AND conrelid = '"discard"'::regclass
  ) THEN
    ALTER TABLE "discard"
      ADD CONSTRAINT chk_discard_quantity_positive
      CHECK (quantity IS NULL OR quantity > 0);
  END IF;
END $$;
-- Historical discarded quantities cannot be inferred reliably and remain NULL.
ALTER TABLE "discard" ADD COLUMN movement_id INTEGER UNIQUE REFERENCES stock_movements(id);
ALTER TABLE stock_products ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE stock_products ADD COLUMN batch VARCHAR(120);
ALTER TABLE stock_products DROP CONSTRAINT IF EXISTS uq_stock_products_product_stock_expire;
CREATE UNIQUE INDEX uq_stock_products_active_product_stock_expire_batch
 ON stock_products(product_id, stock_id, expire_date, COALESCE(batch,'')) WHERE deleted_at IS NULL;
ALTER TABLE stock_products ADD CONSTRAINT chk_stock_products_quantity CHECK(quantity >= 0) NOT VALID;
ALTER TABLE products ADD COLUMN owner_group_id UUID REFERENCES groups(id) ON DELETE RESTRICT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE products ALTER COLUMN image_url TYPE TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS brand VARCHAR(120);
ALTER TABLE products ALTER COLUMN brand TYPE VARCHAR(120);
DROP INDEX IF EXISTS uq_products_name_ci;
CREATE UNIQUE INDEX uq_products_global_name ON products(LOWER(name)) WHERE owner_group_id IS NULL;
CREATE UNIQUE INDEX uq_products_group_name ON products(owner_group_id, LOWER(name)) WHERE owner_group_id IS NOT NULL;
CREATE INDEX idx_stock_movements_summary ON stock_movements(date, purpose, movement_type);
CREATE INDEX idx_stock_products_active_stock ON stock_products(stock_id, expire_date) WHERE deleted_at IS NULL;
