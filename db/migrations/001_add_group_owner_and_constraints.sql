-- ============================================================
-- MIGRATION 001: Add owner_id to groups and enforce 1 active group per owner
-- ============================================================

-- 1. Add owner_id column if not exists
ALTER TABLE groups ADD COLUMN IF NOT EXISTS owner_id UUID;

-- 2. Populate owner_id for existing groups with the first member from user_groups (or fallback)
UPDATE groups g
SET owner_id = (
    SELECT ug.user_id 
    FROM user_groups ug 
    WHERE ug.group_id = g.id 
    ORDER BY ug.created_at ASC 
    LIMIT 1
)
WHERE g.owner_id IS NULL;

-- 3. Set NOT NULL constraint on owner_id
ALTER TABLE groups ALTER COLUMN owner_id SET NOT NULL;

-- 4. Add foreign key referencing users(id)
ALTER TABLE groups 
DROP CONSTRAINT IF EXISTS fk_groups_owner_id_users;

ALTER TABLE groups 
ADD CONSTRAINT fk_groups_owner_id_users 
FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;

-- 5. Create index on owner_id
CREATE INDEX IF NOT EXISTS idx_groups_owner ON groups (owner_id);

-- 6. Enforce at database level that each user/subscriber can have at most ONE active (non-deleted) group
DROP INDEX IF EXISTS uq_groups_one_active_per_owner;

CREATE UNIQUE INDEX uq_groups_one_active_per_owner 
ON groups (owner_id) 
WHERE deleted_at IS NULL;
