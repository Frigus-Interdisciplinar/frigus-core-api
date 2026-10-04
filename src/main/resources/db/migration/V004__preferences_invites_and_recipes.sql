CREATE TABLE user_preferences (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  expiration_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  low_stock_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  shopping_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  weekly_summary_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  theme VARCHAR(16) NOT NULL DEFAULT 'SYSTEM' CHECK (theme IN ('LIGHT','DARK','SYSTEM')),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE user_groups ADD COLUMN member_role VARCHAR(16) NOT NULL DEFAULT 'EDITOR'
 CHECK (member_role IN ('EDITOR','VIEWER'));
CREATE TABLE group_invitations (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
 email VARCHAR(255) NOT NULL,
 name VARCHAR(255),
 member_role VARCHAR(16) NOT NULL CHECK (member_role IN ('EDITOR','VIEWER')),
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMP NOT NULL,
 accepted_at TIMESTAMP,
 revoked_at TIMESTAMP,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_group_invitations_group ON group_invitations(group_id, created_at DESC);
CREATE UNIQUE INDEX uq_group_invitations_pending ON group_invitations(group_id, LOWER(email))
 WHERE accepted_at IS NULL AND revoked_at IS NULL;
CREATE TABLE recipe_favorites (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 recipe_id INTEGER NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(user_id, recipe_id)
);
CREATE INDEX idx_recipe_favorites_user ON recipe_favorites(user_id, created_at DESC);
CREATE TABLE password_reset_tokens (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMP NOT NULL,
 consumed_at TIMESTAMP,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id, created_at DESC);
ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
