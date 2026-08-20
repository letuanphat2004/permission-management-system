ALTER TABLE import_jobs
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER id;

ALTER TABLE import_execution_results
    CHANGE COLUMN business_group principal_name VARCHAR(512) NOT NULL;
