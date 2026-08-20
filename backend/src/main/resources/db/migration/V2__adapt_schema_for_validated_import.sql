ALTER TABLE import_jobs
    DROP INDEX uk_import_jobs_checksum,
    ADD KEY idx_import_jobs_checksum (file_checksum),
    ADD COLUMN storage_path VARCHAR(1024) NULL AFTER file_checksum,
    ADD COLUMN file_size_bytes BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER storage_path,
    CHANGE COLUMN total_rows total_source_rows BIGINT NOT NULL DEFAULT 0,
    CHANGE COLUMN processed_rows total_permission_entries BIGINT NOT NULL DEFAULT 0,
    CHANGE COLUMN valid_rows valid_permission_entries BIGINT NOT NULL DEFAULT 0,
    CHANGE COLUMN error_rows error_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN add_count BIGINT NOT NULL DEFAULT 0 AFTER error_count,
    ADD COLUMN update_count BIGINT NOT NULL DEFAULT 0 AFTER add_count,
    ADD COLUMN remove_count BIGINT NOT NULL DEFAULT 0 AFTER update_count,
    ADD COLUMN skip_count BIGINT NOT NULL DEFAULT 0 AFTER remove_count,
    ADD COLUMN failed_count BIGINT NOT NULL DEFAULT 0 AFTER skip_count,
    ADD COLUMN parsed_at TIMESTAMP(6) NULL AFTER failed_count,
    ADD COLUMN validated_at TIMESTAMP(6) NULL AFTER parsed_at,
    ADD COLUMN execution_started_at TIMESTAMP(6) NULL AFTER validated_at,
    ADD COLUMN executed_at TIMESTAMP(6) NULL AFTER execution_started_at,
    ADD COLUMN failure_message TEXT NULL AFTER executed_at;

CREATE TABLE import_errors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    import_job_id BIGINT NOT NULL,
    source_row_number BIGINT NOT NULL,
    column_name VARCHAR(255) NOT NULL,
    ace_index INT NULL,
    raw_value MEDIUMTEXT NULL,
    error_code VARCHAR(100) NOT NULL,
    error_message VARCHAR(1000) NOT NULL,
    suggestion TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_import_errors_job
        FOREIGN KEY (import_job_id) REFERENCES import_jobs (id)
        ON DELETE CASCADE,
    KEY idx_import_errors_job_row (import_job_id, source_row_number),
    KEY idx_import_errors_job_code (import_job_id, error_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE import_execution_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    import_job_id BIGINT NOT NULL,
    source_row_number BIGINT NOT NULL,
    ace_index INT NULL,
    resource_path VARCHAR(2048) NOT NULL,
    business_group VARCHAR(512) NOT NULL,
    previous_permission VARCHAR(32) NULL,
    desired_permission VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    engine_request_id VARCHAR(255) NULL,
    error_code VARCHAR(100) NULL,
    message TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_import_execution_results_job
        FOREIGN KEY (import_job_id) REFERENCES import_jobs (id)
        ON DELETE CASCADE,
    KEY idx_execution_results_job_status (import_job_id, status),
    KEY idx_execution_results_job_action (import_job_id, action),
    KEY idx_execution_results_engine_request (engine_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
