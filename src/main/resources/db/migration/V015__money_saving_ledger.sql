ALTER TABLE groups ADD COLUMN IF NOT EXISTS time_zone VARCHAR(80) NOT NULL DEFAULT 'America/Sao_Paulo';
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) UNIQUE;
ALTER TABLE users ALTER COLUMN hash_password DROP NOT NULL;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS reason_code VARCHAR(24);
ALTER TABLE "discard" ADD COLUMN IF NOT EXISTS reason_code VARCHAR(24);
ALTER TABLE "discard" ADD COLUMN IF NOT EXISTS canceled_at TIMESTAMPTZ;
ALTER TABLE stock_products ADD COLUMN IF NOT EXISTS purchase_unit_price NUMERIC(10,2);
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_stock_products_purchase_unit_price' AND conrelid = 'stock_products'::regclass) THEN
    ALTER TABLE stock_products ADD CONSTRAINT chk_stock_products_purchase_unit_price CHECK (purchase_unit_price IS NULL OR purchase_unit_price > 0);
  END IF;
END $$;

-- The service already applies the stock delta. Keeping this trigger applies it twice.
DROP TRIGGER IF EXISTS trg_stock_movements_apply ON stock_movements;
DROP FUNCTION IF EXISTS trg_fn_stock_movements_apply();

CREATE TABLE IF NOT EXISTS food_purchases (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  group_id UUID NOT NULL REFERENCES groups(id),
  purchase_date DATE NOT NULL,
  source VARCHAR(24) NOT NULL CHECK (source IN ('USER_CONFIRMED', 'RECEIPT_CAPTURED')),
  receipt_reference VARCHAR(160),
  idempotency_key VARCHAR(160) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  version INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at TIMESTAMPTZ,
  UNIQUE (group_id, idempotency_key)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_purchases_receipt
  ON food_purchases(group_id, receipt_reference)
  WHERE receipt_reference IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS food_purchase_lines (
  id BIGSERIAL PRIMARY KEY,
  purchase_id UUID NOT NULL REFERENCES food_purchases(id),
  stock_key INTEGER NOT NULL,
  stock_product_id INTEGER REFERENCES stock_products(id) ON DELETE SET NULL,
  product_id INTEGER REFERENCES products(id) ON DELETE SET NULL,
  product_name TEXT NOT NULL,
  category VARCHAR(24) NOT NULL,
  category_group VARCHAR(24) NOT NULL,
  unit_of_measure VARCHAR(24) NOT NULL,
  expire_date DATE NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  unit_price NUMERIC(10,2) NOT NULL CHECK (unit_price > 0)
);
CREATE INDEX IF NOT EXISTS idx_food_purchase_lines_purchase ON food_purchase_lines(purchase_id);

-- Supplies include unpriced opening stock and non-purchase stock additions.
CREATE TABLE IF NOT EXISTS money_supplies (
  id BIGSERIAL PRIMARY KEY,
  stock_key INTEGER NOT NULL,
  stock_product_id INTEGER REFERENCES stock_products(id) ON DELETE SET NULL,
  purchase_line_id BIGINT UNIQUE REFERENCES food_purchase_lines(id),
  available_on DATE NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  unknown_cost BOOLEAN NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_money_supplies_stock ON money_supplies(stock_key, available_on, id);

CREATE TABLE IF NOT EXISTS money_exits (
  id BIGSERIAL PRIMARY KEY,
  group_id UUID NOT NULL REFERENCES groups(id),
  stock_key INTEGER NOT NULL,
  stock_product_id INTEGER REFERENCES stock_products(id) ON DELETE SET NULL,
  product_name TEXT NOT NULL,
  category VARCHAR(24) NOT NULL,
  unit_of_measure VARCHAR(24) NOT NULL,
  expire_date DATE NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  local_date DATE NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  kind VARCHAR(24) NOT NULL CHECK (kind IN ('CONSUMED','DISCARDED','OTHER')),
  reason_code VARCHAR(24),
  movement_id INTEGER REFERENCES stock_movements(id) ON DELETE SET NULL,
  discard_id INTEGER,
  canceled_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_money_exits_group_date ON money_exits(group_id, local_date DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_money_exits_stock ON money_exits(stock_key, occurred_at, id);

CREATE TABLE IF NOT EXISTS money_allocations (
  id BIGSERIAL PRIMARY KEY,
  exit_id BIGINT NOT NULL REFERENCES money_exits(id) ON DELETE CASCADE,
  supply_id BIGINT REFERENCES money_supplies(id) ON DELETE SET NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  amount NUMERIC(20,2),
  category_group VARCHAR(24)
);
CREATE INDEX IF NOT EXISTS idx_money_allocations_exit ON money_allocations(exit_id);

CREATE TABLE IF NOT EXISTS food_purchase_revisions (
  id BIGSERIAL PRIMARY KEY,
  purchase_id UUID NOT NULL REFERENCES food_purchases(id),
  previous_version INTEGER NOT NULL,
  previous_state JSONB NOT NULL,
  changed_by UUID NOT NULL REFERENCES users(id),
  changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
