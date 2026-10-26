ALTER TYPE category_enum ADD VALUE IF NOT EXISTS 'FISH';
ALTER TYPE category_enum ADD VALUE IF NOT EXISTS 'FROZEN';
ALTER TYPE category_enum ADD VALUE IF NOT EXISTS 'SWEETS';
ALTER TYPE category_enum ADD VALUE IF NOT EXISTS 'CANNED';
ALTER TYPE category_enum ADD VALUE IF NOT EXISTS 'SEASONING';

ALTER TABLE stock_products ADD COLUMN initial_quantity INTEGER;
UPDATE stock_products SET initial_quantity = quantity WHERE initial_quantity IS NULL;
ALTER TABLE stock_products ALTER COLUMN initial_quantity SET NOT NULL;
ALTER TABLE stock_products ADD CONSTRAINT chk_stock_products_initial_quantity CHECK (initial_quantity >= 0);

ALTER TABLE notifications ADD COLUMN deleted_at TIMESTAMP;
CREATE INDEX idx_notifications_recipient_active_created
    ON notifications(recipient_user_id, created_at DESC) WHERE deleted_at IS NULL;
