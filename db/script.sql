-- ============================================================
-- EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- DROP TABLES
-- ============================================================
DROP TABLE IF EXISTS transaction_events CASCADE;

DROP TABLE IF EXISTS transactions CASCADE;

DROP TABLE IF EXISTS subscriptions CASCADE;

DROP TABLE IF EXISTS plans CASCADE;

DROP TABLE IF EXISTS requests CASCADE;

DROP TABLE IF EXISTS message_reads CASCADE;

DROP TABLE IF EXISTS message_attachments CASCADE;

DROP TABLE IF EXISTS messages CASCADE;

DROP TABLE IF EXISTS conversation_participants CASCADE;

DROP TABLE IF EXISTS stock_movements CASCADE;

DROP TABLE IF EXISTS shopping_list_products CASCADE;

DROP TABLE IF EXISTS "discard" CASCADE;

DROP TABLE IF EXISTS conversations CASCADE;

DROP TABLE IF EXISTS shopping_lists CASCADE;

DROP TABLE IF EXISTS recipe_suggestions CASCADE;

DROP TABLE IF EXISTS recipe_ingredients CASCADE;

DROP TABLE IF EXISTS stock_products CASCADE;

DROP TABLE IF EXISTS user_groups CASCADE;

DROP TABLE IF EXISTS products CASCADE;

DROP TABLE IF EXISTS stocks CASCADE;

DROP TABLE IF EXISTS users CASCADE;

DROP TABLE IF EXISTS recipes CASCADE;

DROP TABLE IF EXISTS groups CASCADE;

-- ============================================================
-- ENUMS
-- ============================================================
DROP TYPE IF EXISTS message_type_enum CASCADE;

DROP TYPE IF EXISTS conversation_type_enum CASCADE;

DROP TYPE IF EXISTS movement_type_enum CASCADE;

DROP TYPE IF EXISTS product_list_status_enum CASCADE;

DROP TYPE IF EXISTS list_status_enum CASCADE;

DROP TYPE IF EXISTS product_status_enum CASCADE;

DROP TYPE IF EXISTS storage_place_enum CASCADE;

DROP TYPE IF EXISTS category_enum CASCADE;

DROP TYPE IF EXISTS account_type_enum CASCADE;

DROP TYPE IF EXISTS billing_interval_enum CASCADE;

DROP TYPE IF EXISTS user_role_enum CASCADE;

DROP TYPE IF EXISTS payment_method_enum CASCADE;

DROP TYPE IF EXISTS subscription_status_enum CASCADE;

DROP TYPE IF EXISTS transaction_status_enum CASCADE;

DROP TYPE IF EXISTS unit_of_measure_enum CASCADE;

CREATE TYPE unit_of_measure_enum AS ENUM(
  'KILOGRAM',
  'GRAM',
  'LITER',
  'MILLILITER',
  'UNIT',
  'DOZEN',
  'PACKAGE'
);

CREATE TYPE account_type_enum AS ENUM('DOMESTIC', 'BUSINESS', 'COMMERCIAL');

CREATE TYPE user_role_enum AS ENUM('USER', 'ADMIN');

CREATE TYPE category_enum AS ENUM(
  'FRUIT',
  'VEGETABLE',
  'DAIRY',
  'MEAT',
  'GRAIN',
  'BEVERAGE',
  'CLEANING',
  'PERSONAL_HYGIENE'
);

CREATE TYPE storage_place_enum AS ENUM('FRIDGE', 'FREEZER', 'PANTRY', 'CABINET', 'SHELF');

CREATE TYPE product_status_enum AS ENUM('FRESH', 'NEAR_EXPIRATION', 'EXPIRED');

CREATE TYPE list_status_enum AS ENUM('OPEN', 'COMPLETED', 'CANCELED');

CREATE TYPE product_list_status_enum AS ENUM('PENDING', 'PURCHASED', 'REMOVED');

CREATE TYPE movement_type_enum AS ENUM('IN', 'OUT', 'ADJUSTMENT');

CREATE TYPE conversation_type_enum AS ENUM('PRIVATE', 'GROUP');

CREATE TYPE message_type_enum AS ENUM('TEXT', 'IMAGE', 'SYSTEM');

CREATE TYPE billing_interval_enum AS ENUM('MONTHLY', 'YEARLY');

CREATE TYPE payment_method_enum AS ENUM('CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'BOLETO');

CREATE TYPE subscription_status_enum AS ENUM(
  'TRIAL',
  'ACTIVE',
  'CANCELED',
  'EXPIRED',
  'DELINQUENT'
);

CREATE TYPE transaction_status_enum AS ENUM(
  'PENDING',
  'PROCESSING',
  'APPROVED',
  'REJECTED',
  'CANCELED',
  'ERROR'
);

-- ============================================================
-- TABLES
-- ============================================================
CREATE TABLE groups (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR NOT NULL,
  banner_picture TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE recipes (
  id SERIAL PRIMARY KEY,
  name VARCHAR NOT NULL,
  description TEXT,
  instructions TEXT,
  domestic_only BOOLEAN NOT NULL DEFAULT TRUE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR NOT NULL,
  birth_date DATE,
  account_type account_type_enum NOT NULL,
  role user_role_enum NOT NULL DEFAULT 'USER',
  email VARCHAR NOT NULL,
  hash_password VARCHAR NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE stocks (
  id SERIAL PRIMARY KEY,
  group_id UUID NOT NULL,
  name VARCHAR NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  CONSTRAINT fk_stocks_group_id_groups FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE
);

CREATE TABLE products (
  id SERIAL PRIMARY KEY,
  name VARCHAR NOT NULL,
  category category_enum NOT NULL,
  storage_place storage_place_enum NOT NULL,
  unit_price NUMERIC(10, 2) NOT NULL,
  unit_of_measure unit_of_measure_enum NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_groups (
  id SERIAL PRIMARY KEY,
  user_id UUID NOT NULL,
  group_id UUID NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_user_groups_user_group UNIQUE (user_id, group_id),
  CONSTRAINT fk_user_groups_user_id_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_groups_group_id_groups FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE
);

CREATE TABLE stock_products (
  id SERIAL PRIMARY KEY,
  product_id INTEGER NOT NULL,
  stock_id INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  minimal_quantity INTEGER,
  expire_date DATE NOT NULL,
  product_status product_status_enum,
  category category_enum NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_stock_products_product_stock_expire UNIQUE (product_id, stock_id, expire_date),
  CONSTRAINT fk_stock_products_product_id_products FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_stock_products_stock_id_stocks FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE
);

CREATE TABLE recipe_ingredients (
  id SERIAL PRIMARY KEY,
  recipe_id INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  quantity DECIMAL,
  unit VARCHAR,
  required BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_recipe_ingredients_recipe_product UNIQUE (recipe_id, product_id),
  CONSTRAINT fk_recipe_ingredients_recipe_id_recipes FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
  CONSTRAINT fk_recipe_ingredients_product_id_products FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE recipe_suggestions (
  id SERIAL PRIMARY KEY,
  recipe_id INTEGER NOT NULL,
  stock_id INTEGER NOT NULL,
  matched_ingredients INTEGER NOT NULL DEFAULT 0,
  missing_ingredients INTEGER NOT NULL DEFAULT 0,
  nearest_expire_date DATE,
  score DECIMAL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_recipe_suggestions_recipe_id_recipes FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
  CONSTRAINT fk_recipe_suggestions_stock_id_stocks FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE
);

CREATE TABLE shopping_lists (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  date DATE NOT NULL DEFAULT CURRENT_DATE,
  stock_id INTEGER NOT NULL,
  status list_status_enum NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_shopping_lists_stock_id_stocks FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE
);

CREATE TABLE conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_type conversation_type_enum NOT NULL,
  group_id UUID,
  name VARCHAR,
  pair_key VARCHAR,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_pair UNIQUE (pair_key),
  CONSTRAINT fk_conversations_group_id_groups FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE SET NULL,
  CONSTRAINT chk_conversation_type_group CHECK (
    (
      conversation_type = 'GROUP'
      AND group_id IS NOT NULL
    )
    OR (
      conversation_type = 'PRIVATE'
      AND group_id IS NULL
    )
  )
);

CREATE TABLE "discard" (
  id SERIAL PRIMARY KEY,
  stock_product_id INTEGER NOT NULL,
  reason TEXT,
  date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_discard_stock_product_id_stock_products FOREIGN KEY (stock_product_id) REFERENCES stock_products (id) ON DELETE CASCADE
);

CREATE TABLE shopping_list_products (
  id SERIAL PRIMARY KEY,
  list_id UUID NOT NULL,
  product_id INTEGER NOT NULL,
  status product_list_status_enum,
  quantity INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_shopping_list_products_list_product UNIQUE (list_id, product_id),
  CONSTRAINT fk_shopping_list_products_list_id_shopping_lists FOREIGN KEY (list_id) REFERENCES shopping_lists (id) ON DELETE CASCADE,
  CONSTRAINT fk_shopping_list_products_product_id_products FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE stock_movements (
  id SERIAL PRIMARY KEY,
  stock_product_id INTEGER NOT NULL,
  user_id UUID NOT NULL,
  movement_type movement_type_enum NOT NULL,
  quantity INTEGER NOT NULL,
  date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_stock_movements_stock_product_id_stock_products FOREIGN KEY (stock_product_id) REFERENCES stock_products (id) ON DELETE CASCADE,
  CONSTRAINT fk_stock_movements_user_id_users FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE conversation_participants (
  conversation_id UUID NOT NULL,
  user_id UUID NOT NULL,
  joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  left_at TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (conversation_id, user_id),
  CONSTRAINT fk_conversation_participants_conversation_id_conversations FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
  CONSTRAINT fk_conversation_participants_user_id_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE messages (
  id SERIAL PRIMARY KEY,
  conversation_id UUID NOT NULL,
  sender_id UUID NOT NULL,
  message_type message_type_enum NOT NULL,
  content TEXT,
  related_shopping_list_product_id INTEGER,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_messages_id_conversation UNIQUE (id, conversation_id),
  CONSTRAINT fk_messages_conversation_id_conversations FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
  CONSTRAINT fk_messages_sender_id_users FOREIGN KEY (sender_id) REFERENCES users (id),
  CONSTRAINT fk_messages_related_shopping_list_product_id_slp FOREIGN KEY (related_shopping_list_product_id) REFERENCES shopping_list_products (id) ON DELETE SET NULL,
  CONSTRAINT fk_messages_conversation_id_participants FOREIGN KEY (conversation_id, sender_id) REFERENCES conversation_participants (conversation_id, user_id)
);

CREATE TABLE message_attachments (
  id SERIAL PRIMARY KEY,
  message_id INTEGER NOT NULL,
  file_url TEXT NOT NULL,
  file_name VARCHAR,
  mime_type VARCHAR NOT NULL,
  file_size INTEGER,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_message_attachments_message_id_messages FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE
);

CREATE TABLE message_reads (
  message_id INTEGER NOT NULL,
  conversation_id UUID NOT NULL,
  user_id UUID NOT NULL,
  read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id, user_id),
  CONSTRAINT fk_message_reads_message_id_messages FOREIGN KEY (message_id, conversation_id) REFERENCES messages (id, conversation_id) ON DELETE CASCADE,
  CONSTRAINT fk_message_reads_conversation_id_participants FOREIGN KEY (conversation_id, user_id) REFERENCES conversation_participants (conversation_id, user_id) ON DELETE CASCADE
);

CREATE TABLE requests (
  id SERIAL PRIMARY KEY,
  user_id UUID NOT NULL,
  product_id INTEGER NOT NULL,
  stock_id INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  description TEXT,
  picture TEXT,
  date DATE NOT NULL DEFAULT CURRENT_DATE,
  conversation_id UUID,
  message_id INTEGER,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_requests_user_id_users FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_requests_product_id_products FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_requests_stock_id_stocks FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE,
  CONSTRAINT fk_requests_conversation_id_conversations FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE SET NULL,
  CONSTRAINT fk_requests_message_id_messages FOREIGN KEY (message_id, conversation_id) REFERENCES messages (id, conversation_id) ON DELETE SET NULL,
  CONSTRAINT chk_requests_message_conversation CHECK (
    message_id IS NULL
    OR conversation_id IS NOT NULL
  )
);

CREATE TABLE plans (
  id SERIAL PRIMARY KEY,
  plan_code VARCHAR(20) NOT NULL,
  name VARCHAR NOT NULL,
  description TEXT,
  price NUMERIC(10, 2) NOT NULL DEFAULT 0,
  billing_interval billing_interval_enum,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE subscriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  plan_id INTEGER NOT NULL,
  status subscription_status_enum NOT NULL DEFAULT 'TRIAL',
  started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  current_period_start TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  current_period_end TIMESTAMP,
  canceled_at TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  CONSTRAINT fk_subscriptions_user_id_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_subscriptions_plan_id_plans FOREIGN KEY (plan_id) REFERENCES plans (id)
);

CREATE TABLE transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  idempotency_key VARCHAR(120) NOT NULL,
  user_id UUID NOT NULL,
  subscription_id UUID NOT NULL,
  plan_id INTEGER NOT NULL,
  amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
  payment_method payment_method_enum NOT NULL,
  fake_card_last4 VARCHAR(4),
  fake_pix_key VARCHAR,
  status transaction_status_enum NOT NULL DEFAULT 'PENDING',
  queue_job_id VARCHAR,
  attempts INTEGER NOT NULL DEFAULT 0,
  max_attempts INTEGER NOT NULL DEFAULT 3,
  error_message TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  queued_at TIMESTAMP,
  processed_at TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_transactions_user_id_users FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_transactions_subscription_id_subscriptions FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE CASCADE,
  CONSTRAINT uq_transactions_idempotency_key UNIQUE (idempotency_key),
  CONSTRAINT fk_transactions_plan_id_plans FOREIGN KEY (plan_id) REFERENCES plans (id),
  CONSTRAINT chk_transactions_processed_at CHECK (
    (
      status IN ('PENDING', 'PROCESSING')
      AND processed_at IS NULL
    )
    OR (status NOT IN ('PENDING', 'PROCESSING'))
  )
);

CREATE TABLE transaction_events (
  id SERIAL PRIMARY KEY,
  transaction_id UUID NOT NULL,
  status transaction_status_enum NOT NULL,
  message TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_transaction_events_transaction_id_transactions FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_user_groups_user ON user_groups (user_id);

CREATE INDEX idx_user_groups_group ON user_groups (group_id);

CREATE INDEX idx_stocks_group ON stocks (group_id);

CREATE INDEX idx_stocks_deleted_at ON stocks (deleted_at);

CREATE INDEX idx_products_category ON products (category);

CREATE INDEX idx_products_storage_place ON products (storage_place);

CREATE INDEX idx_stock_products_product ON stock_products (product_id);

CREATE INDEX idx_stock_products_stock ON stock_products (stock_id);

CREATE INDEX idx_stock_products_expire_date ON stock_products (expire_date);

CREATE INDEX idx_stock_products_status ON stock_products (product_status);

CREATE INDEX idx_recipe_ingredients_recipe ON recipe_ingredients (recipe_id);

CREATE INDEX idx_recipe_ingredients_product ON recipe_ingredients (product_id);

CREATE INDEX idx_recipe_suggestions_recipe ON recipe_suggestions (recipe_id);

CREATE INDEX idx_recipe_suggestions_stock ON recipe_suggestions (stock_id);

CREATE INDEX idx_recipe_suggestions_expire_date ON recipe_suggestions (nearest_expire_date);

CREATE INDEX idx_shopping_lists_stock ON shopping_lists (stock_id);

CREATE INDEX idx_shopping_list_products_list ON shopping_list_products (list_id);

CREATE INDEX idx_shopping_list_products_product ON shopping_list_products (product_id);

CREATE INDEX idx_stock_movements_stock_product ON stock_movements (stock_product_id);

CREATE INDEX idx_stock_movements_user ON stock_movements (user_id);

CREATE INDEX idx_stock_movements_date ON stock_movements (date);

CREATE INDEX idx_conversations_group ON conversations (group_id);

CREATE INDEX idx_conversation_participants_user ON conversation_participants (user_id);

CREATE INDEX idx_messages_conversation_created ON messages (conversation_id, created_at);

CREATE INDEX idx_messages_sender ON messages (sender_id);

CREATE INDEX idx_message_attachments_message ON message_attachments (message_id);

CREATE INDEX idx_message_reads_user ON message_reads (user_id);

CREATE INDEX idx_requests_user ON requests (user_id);

CREATE INDEX idx_requests_product ON requests (product_id);

CREATE INDEX idx_requests_stock ON requests (stock_id);

CREATE INDEX idx_requests_conversation ON requests (conversation_id);

CREATE INDEX idx_subscriptions_user ON subscriptions (user_id);

CREATE INDEX idx_subscriptions_plan ON subscriptions (plan_id);

CREATE INDEX idx_subscriptions_status ON subscriptions (status);

CREATE INDEX idx_subscriptions_deleted_at ON subscriptions (deleted_at);

CREATE UNIQUE INDEX uq_subscriptions_one_active_per_user ON subscriptions (user_id)
WHERE
  status IN ('TRIAL', 'ACTIVE')
  AND deleted_at IS NULL;

CREATE INDEX idx_transactions_user ON transactions (user_id);

CREATE INDEX idx_transactions_subscription ON transactions (subscription_id);

CREATE INDEX idx_transactions_plan ON transactions (plan_id);

CREATE INDEX idx_transactions_status ON transactions (status);

CREATE INDEX idx_transactions_created_at ON transactions (created_at);

CREATE INDEX idx_transaction_events_transaction ON transaction_events (transaction_id, created_at);

CREATE UNIQUE INDEX uq_users_email_active ON users (email)
WHERE
  deleted_at IS NULL;

CREATE INDEX idx_users_deleted_at ON users (deleted_at);

CREATE UNIQUE INDEX uq_plans_name_active ON plans (name)
WHERE
  deleted_at IS NULL;

CREATE UNIQUE INDEX uq_plans_code_active ON plans (plan_code)
WHERE
  deleted_at IS NULL;

CREATE INDEX idx_plans_deleted_at ON plans (deleted_at);

CREATE INDEX idx_groups_deleted_at ON groups (deleted_at);

-- ============================================================
-- FUNCTIONS & TRIGGERS
-- ============================================================
CREATE OR REPLACE FUNCTION fn_set_updated_at () RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
tbl TEXT;
    tables TEXT[] := ARRAY[
        'groups', 'recipes', 'users', 'stocks', 'products',
        'user_groups', 'stock_products', 'recipe_ingredients',
        'recipe_suggestions', 'shopping_lists', 'conversations',
        'discard', 'shopping_list_products', 'conversation_participants',
        'messages', 'message_attachments', 'requests',
        'plans', 'subscriptions', 'transactions'
    ];
BEGIN
    FOREACH tbl IN ARRAY tables LOOP
        EXECUTE format(
            'DROP TRIGGER IF EXISTS trg_set_updated_at ON %I;',
            tbl
        );
EXECUTE format(
        'CREATE TRIGGER trg_set_updated_at BEFORE UPDATE ON %I
         FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();',
        tbl
        );
END LOOP;
END $$;

CREATE OR REPLACE FUNCTION fn_log_transaction_status_change () RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' OR NEW.status IS DISTINCT FROM OLD.status THEN
        INSERT INTO transaction_events (transaction_id, status, message)
        VALUES (NEW.id, NEW.status, NULL);
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_log_transaction_status_change ON transactions;

CREATE TRIGGER trg_log_transaction_status_change
AFTER INSERT OR UPDATE ON transactions FOR EACH ROW
EXECUTE FUNCTION fn_log_transaction_status_change ();

-- ============================================================
-- SEEDS
-- ============================================================
INSERT INTO
  plans (
    plan_code,
    name,
    description,
    price,
    billing_interval,
    active
  )
VALUES
  (
    'FREE',
    'Frigus Free',
    'Plano gratuito com funcionalidades básicas de controle de estoque.',
    0.00,
    NULL,
    TRUE
  ),
  (
    'PLUS',
    'Frigus Plus',
    'Plano para uso doméstico compartilhado entre membros da família.',
    29.99,
    'MONTHLY',
    TRUE
  ),
  (
    'FAMILY',
    'Frigus Família',
    'Plano compartilhado para famílias maiores.',
    49.99,
    'MONTHLY',
    TRUE
  ),
  (
    'COMMERCIAL',
    'Frigus Comercial',
    'Plano para pequenos comércios com múltiplos estoques.',
    119.99,
    'MONTHLY',
    TRUE
  ),
  (
    'ENTERPRISE',
    'Frigus Empresarial',
    'Plano para empresas com necessidades avançadas de gestão.',
    159.99,
    'MONTHLY',
    TRUE
  );
