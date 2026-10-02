-- Notifications, stock minimum limit and commercial stock-alert trigger.
-- This migration is safe to execute once on an existing PostgreSQL database.

BEGIN;

ALTER TABLE stock_products
    ADD COLUMN IF NOT EXISTS minimal_quantity INTEGER;

ALTER TABLE stock_products
    DROP CONSTRAINT IF EXISTS chk_stock_products_minimal_quantity;

ALTER TABLE stock_products
    ADD CONSTRAINT chk_stock_products_minimal_quantity
    CHECK (minimal_quantity IS NULL OR minimal_quantity >= 0);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_type_enum') THEN
        CREATE TYPE notification_type_enum AS ENUM (
            'PRODUCT_NEAR_EXPIRATION',
            'GROUP_MEMBER_JOINED',
            'SHOPPING_LIST_PRODUCT_ADDED',
            'SHOPPING_LIST_REMINDER',
            'LOW_STOCK',
            'OUT_OF_STOCK',
            'AD_CLICK_MILESTONE'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL,
    group_id UUID,
    type notification_type_enum NOT NULL,
    title VARCHAR NOT NULL,
    body TEXT NOT NULL,
    reference_id VARCHAR(120),
    deduplication_key VARCHAR(220),
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_recipient_user_id_users
        FOREIGN KEY (recipient_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_group_id_groups
        FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created_at
    ON notifications (recipient_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_group
    ON notifications (group_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unread
    ON notifications (recipient_user_id)
    WHERE read_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_deduplication_key
    ON notifications (deduplication_key)
    WHERE deduplication_key IS NOT NULL;

-- Commercial accounts receive low/zero-stock notifications on the same
-- transaction that changes the stock product. Transition checks prevent
-- duplicate alerts while the quantity remains under the threshold.
CREATE OR REPLACE FUNCTION trg_fn_notify_commercial_stock_level()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_group_id UUID;
    v_is_commercial BOOLEAN;
    v_type notification_type_enum;
    v_title VARCHAR;
    v_body TEXT;
BEGIN
    SELECT s.group_id, u.account_type = 'COMMERCIAL'
      INTO v_group_id, v_is_commercial
      FROM stocks s
      JOIN groups g ON g.id = s.group_id
      JOIN users u ON u.id = g.owner_id
     WHERE s.id = NEW.stock_id;

    IF NOT COALESCE(v_is_commercial, FALSE) THEN
        RETURN NEW;
    END IF;

    IF NEW.quantity = 0
       AND (TG_OP = 'INSERT' OR OLD.quantity IS DISTINCT FROM 0) THEN
        v_type := 'OUT_OF_STOCK';
        v_title := 'Estoque zerado';
        v_body := 'Um produto ficou sem unidades no estoque.';
    ELSIF NEW.minimal_quantity IS NOT NULL
       AND NEW.quantity <= NEW.minimal_quantity
       AND (TG_OP = 'INSERT'
            OR OLD.quantity > OLD.minimal_quantity
            OR OLD.minimal_quantity IS DISTINCT FROM NEW.minimal_quantity) THEN
        v_type := 'LOW_STOCK';
        v_title := 'Produto com pouca quantidade no estoque';
        v_body := 'Um produto atingiu o limite mínimo configurado.';
    ELSE
        RETURN NEW;
    END IF;

    INSERT INTO notifications (recipient_user_id, group_id, type, title, body, reference_id)
    SELECT ug.user_id, v_group_id, v_type, v_title, v_body, NEW.id::VARCHAR
      FROM user_groups ug
     WHERE ug.group_id = v_group_id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notify_commercial_stock_level ON stock_products;
CREATE TRIGGER trg_notify_commercial_stock_level
AFTER INSERT OR UPDATE OF quantity, minimal_quantity ON stock_products
FOR EACH ROW EXECUTE FUNCTION trg_fn_notify_commercial_stock_level();

-- Adding an item to a domestic/commercial shopping list notifies the group.
CREATE OR REPLACE FUNCTION trg_fn_notify_shopping_list_product_added()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_group_id UUID;
BEGIN
    SELECT s.group_id INTO v_group_id
      FROM shopping_lists sl
      JOIN stocks s ON s.id = sl.stock_id
      JOIN groups g ON g.id = s.group_id
      JOIN users u ON u.id = g.owner_id
     WHERE sl.id = NEW.list_id
       AND u.account_type IN ('DOMESTIC', 'COMMERCIAL');

    IF v_group_id IS NULL THEN
        RETURN NEW;
    END IF;

    INSERT INTO notifications (
        recipient_user_id, group_id, type, title, body, reference_id, deduplication_key
    )
    SELECT ug.user_id, v_group_id, 'SHOPPING_LIST_PRODUCT_ADDED',
           'Item adicionado à lista de compras',
           'Um novo item foi adicionado à lista de compras do grupo.',
           NEW.id::VARCHAR,
           'shopping-list-product:' || NEW.id::VARCHAR || ':' || ug.user_id::VARCHAR
      FROM user_groups ug
     WHERE ug.group_id = v_group_id
    ON CONFLICT (deduplication_key) WHERE deduplication_key IS NOT NULL DO NOTHING;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notify_shopping_list_product_added ON shopping_list_products;
CREATE TRIGGER trg_notify_shopping_list_product_added
AFTER INSERT ON shopping_list_products
FOR EACH ROW EXECUTE FUNCTION trg_fn_notify_shopping_list_product_added();

COMMIT;
