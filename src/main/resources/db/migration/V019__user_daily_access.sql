CREATE SCHEMA IF NOT EXISTS dw;

CREATE TABLE dw.user_daily_access (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_date DATE NOT NULL,
    first_access_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, access_date)
);

CREATE INDEX idx_user_daily_access_date ON dw.user_daily_access (access_date);
