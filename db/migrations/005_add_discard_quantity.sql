-- Existing discard rows predate the stored quantity. Keep them null rather than
-- inventing a historical value; new discards always include their quantity.
ALTER TABLE "discard" ADD COLUMN IF NOT EXISTS quantity INTEGER;
