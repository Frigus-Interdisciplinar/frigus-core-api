ALTER TABLE receipt_draft_lines
    ADD COLUMN normalized_product_name VARCHAR(255),
    ADD COLUMN brand VARCHAR(120),
    ADD COLUMN category VARCHAR(30),
    ADD COLUMN storage_place VARCHAR(30),
    ADD COLUMN unit_of_measure VARCHAR(30);
