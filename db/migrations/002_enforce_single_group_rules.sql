-- ============================================================
-- MIGRATION 002: Enforce Single Group per User Rules
-- 1 pessoa só pode fazer parte de um grupo ativo por vez
-- 1 pessoa só pode ser dona de 1 grupo ativo por vez
-- ============================================================

-- Index to optimize querying active group memberships by user_id
CREATE INDEX IF NOT EXISTS idx_user_groups_user_id ON user_groups (user_id);
CREATE INDEX IF NOT EXISTS idx_user_groups_group_id ON user_groups (group_id);
CREATE INDEX IF NOT EXISTS idx_groups_deleted_at ON groups (deleted_at);
