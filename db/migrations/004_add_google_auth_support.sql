-- Migration 004: Suporte a Login Social com Google
-- Adiciona coluna google_id para identificador único da conta Google
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) UNIQUE;

-- Torna hash_password opcional para usuários que se cadastram diretamente via Google
ALTER TABLE users ALTER COLUMN hash_password DROP NOT NULL;
