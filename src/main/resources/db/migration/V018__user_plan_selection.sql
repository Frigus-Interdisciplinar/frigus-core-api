ALTER TABLE users
    ADD COLUMN plan_selection_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN pending_plan_transaction_id UUID;
