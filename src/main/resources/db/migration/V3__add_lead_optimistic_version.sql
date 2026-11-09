-- Additive change. Existing leads retain status/history and start at version 0.
-- Keep a default for historical SQL clients and the test fixtures that insert
-- directly. Spring Data JPA updates the version atomically on each mutation.
ALTER TABLE leads ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
