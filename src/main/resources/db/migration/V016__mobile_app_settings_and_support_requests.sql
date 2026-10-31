CREATE TABLE user_app_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    dietary_preferences JSONB NOT NULL DEFAULT '[]'::jsonb,
    dietary_restrictions JSONB NOT NULL DEFAULT '[]'::jsonb,
    quick_recipes_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    budget_recipes_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    weight_unit VARCHAR(8) NOT NULL DEFAULT 'KG',
    volume_unit VARCHAR(8) NOT NULL DEFAULT 'L',
    temperature_unit VARCHAR(12) NOT NULL DEFAULT 'CELSIUS',
    usage_data_collection_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    personalized_suggestions_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE support_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    protocol VARCHAR(16) NOT NULL UNIQUE,
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')),
    subject VARCHAR(120) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_support_requests_user_created ON support_requests(user_id, created_at DESC);
