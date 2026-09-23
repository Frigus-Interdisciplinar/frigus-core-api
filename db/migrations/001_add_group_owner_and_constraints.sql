-- ============================================================
-- MIGRATION 001: Add owner_id to groups
--
-- Existing data contains one soft-deleted group without members and active
-- groups that share inferred owners. Therefore this migration preserves the
-- historical records while enforcing that every active group has an owner.
-- ============================================================

BEGIN;

-- 1. Add owner_id column if not exists.
-- It stays nullable for historical soft-deleted groups that no longer have
-- a member from whom an owner can be inferred.
ALTER TABLE groups ADD COLUMN IF NOT EXISTS owner_id UUID;

-- 2. Populate owner_id for groups that have members, using the first member.
UPDATE groups g
SET owner_id = (
    SELECT ug.user_id 
    FROM user_groups ug 
    WHERE ug.group_id = g.id 
    ORDER BY ug.created_at ASC 
    LIMIT 1
)
WHERE g.owner_id IS NULL;

-- 3. Require an owner for active groups. A soft-deleted historical group may
-- remain without an owner, preventing this migration from inventing data.
ALTER TABLE groups
DROP CONSTRAINT IF EXISTS chk_active_groups_require_owner;

ALTER TABLE groups
ADD CONSTRAINT chk_active_groups_require_owner
CHECK (deleted_at IS NOT NULL OR owner_id IS NOT NULL);

-- 4. Add foreign key referencing users(id).
ALTER TABLE groups 
DROP CONSTRAINT IF EXISTS fk_groups_owner_id_users;

ALTER TABLE groups 
ADD CONSTRAINT fk_groups_owner_id_users 
FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;

-- 5. Create index on owner_id
CREATE INDEX IF NOT EXISTS idx_groups_owner ON groups (owner_id);

-- 6. The one-active-group-per-owner index is intentionally deferred. Current
-- historical records do not satisfy it; application rules already prevent
-- new duplicates. Add it in a later migration after data cleanup.

COMMIT;
