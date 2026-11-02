CREATE TABLE advertisements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advertiser_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    product_name VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    destination_url VARCHAR(2048),
    price NUMERIC(12,2) CHECK (price IS NULL OR price >= 0),
    start_date DATE,
    end_date DATE,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED')),
    impressions BIGINT NOT NULL DEFAULT 0 CHECK (impressions >= 0),
    clicks BIGINT NOT NULL DEFAULT 0 CHECK (clicks >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_advertisement_dates CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_advertisements_owner_updated ON advertisements(advertiser_id, updated_at DESC);
CREATE INDEX idx_advertisements_active_dates ON advertisements(status, start_date, end_date) WHERE status = 'ACTIVE';
