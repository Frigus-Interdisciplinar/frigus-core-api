CREATE SCHEMA IF NOT EXISTS dw;

CREATE TABLE IF NOT EXISTS dw.user_daily_access (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_date DATE NOT NULL,
    first_access_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, access_date)
);

-- Some Supabase environments already contain this table outside Flyway history.
-- Keep their schema compatible while allowing Flyway to record version 019.
ALTER TABLE dw.user_daily_access
    ALTER COLUMN first_access_at SET DEFAULT CURRENT_TIMESTAMP;

-- The composite index also supports lookups by access_date alone.
CREATE INDEX IF NOT EXISTS ix_user_daily_access_date_user
    ON dw.user_daily_access (access_date, user_id);
