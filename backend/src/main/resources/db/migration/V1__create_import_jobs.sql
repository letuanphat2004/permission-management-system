CREATE TABLE import_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    file_checksum CHAR(64) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_rows BIGINT NOT NULL DEFAULT 0,
    processed_rows BIGINT NOT NULL DEFAULT 0,
    valid_rows BIGINT NOT NULL DEFAULT 0,
    error_rows BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_import_jobs_checksum (file_checksum),
    KEY idx_import_jobs_status (status),
    KEY idx_import_jobs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

