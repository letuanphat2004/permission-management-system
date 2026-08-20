ALTER TABLE import_jobs
    ADD COLUMN skipped_permission_entries BIGINT NOT NULL DEFAULT 0 AFTER valid_permission_entries;
