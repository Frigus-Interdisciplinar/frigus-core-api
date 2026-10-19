-- Adiciona campos added_by na shopping_lists e shopping_list_products,
-- e campo notes na shopping_list_products para armazenar observações e origem do item.

BEGIN;

ALTER TABLE shopping_lists
    ADD COLUMN IF NOT EXISTS added_by VARCHAR(50) NOT NULL DEFAULT 'Adicionado por você';

ALTER TABLE shopping_list_products
    ADD COLUMN IF NOT EXISTS notes VARCHAR(255),
    ADD COLUMN IF NOT EXISTS added_by VARCHAR(50) NOT NULL DEFAULT 'Adicionado por você';

COMMIT;
