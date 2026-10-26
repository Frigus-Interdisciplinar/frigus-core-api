CREATE TABLE receipt_drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_id INTEGER NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    access_key VARCHAR(44),
    issuer_name VARCHAR(255),
    issued_at TIMESTAMPTZ,
    total_amount NUMERIC(12,2),
    warnings JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    imported_at TIMESTAMPTZ,
    UNIQUE (stock_id, access_key)
);
CREATE TABLE receipt_draft_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL REFERENCES receipt_drafts(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit VARCHAR(30),
    unit_price NUMERIC(12,2),
    total_price NUMERIC(12,2),
    confidence DOUBLE PRECISION CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
    evidence VARCHAR(1000),
    matched_product_id INTEGER REFERENCES products(id),
    expire_date DATE
);
CREATE INDEX idx_receipt_drafts_owner ON receipt_drafts(created_by, created_at DESC);
CREATE INDEX idx_receipt_draft_lines_receipt ON receipt_draft_lines(receipt_id);
