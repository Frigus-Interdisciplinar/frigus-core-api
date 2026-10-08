-- Migration 005: Add quantity column to discard table
ALTER TABLE "discard" ADD COLUMN IF NOT EXISTS quantity INTEGER NOT NULL DEFAULT 1;
