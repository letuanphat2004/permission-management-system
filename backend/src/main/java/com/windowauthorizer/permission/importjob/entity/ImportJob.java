package com.windowauthorizer.permission.importjob.entity;

import com.windowauthorizer.permission.importjob.domain.ImportJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "import_jobs")
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    @Column(name = "file_checksum", nullable = false, length = 64)
    private String fileChecksum;

    @Column(name = "storage_path", length = 1024)
    private String storagePath;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ImportJobStatus status;

    @Column(name = "total_source_rows", nullable = false)
    private long totalSourceRows;

    @Column(name = "total_permission_entries", nullable = false)
    private long totalPermissionEntries;

    @Column(name = "valid_permission_entries", nullable = false)
    private long validPermissionEntries;

    @Column(name = "skipped_permission_entries", nullable = false)
    private long skippedPermissionEntries;

    @Column(name = "error_count", nullable = false)
    private long errorCount;

    @Column(name = "add_count", nullable = false)
    private long addCount;

    @Column(name = "update_count", nullable = false)
    private long updateCount;

    @Column(name = "remove_count", nullable = false)
    private long removeCount;

    @Column(name = "skip_count", nullable = false)
    private long skipCount;

    @Column(name = "failed_count", nullable = false)
    private long failedCount;

    @Column(name = "parsed_at")
    private Instant parsedAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "execution_started_at")
    private Instant executionStartedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected ImportJob() {
    }

    public static ImportJob parsing(String fileName, String checksum, String storagePath, long fileSizeBytes,
                                    String createdBy) {
        ImportJob job = new ImportJob();
        job.jobType = "PERMISSION_IMPORT";
        job.fileName = fileName;
        job.fileChecksum = checksum;
        job.storagePath = storagePath;
        job.fileSizeBytes = fileSizeBytes;
        job.status = ImportJobStatus.PARSING;
        job.createdBy = createdBy;
        job.createdAt = Instant.now();
        job.updatedAt = job.createdAt;
        return job;
    }

    public void completeValidation(long totalSourceRows, long totalPermissionEntries,
                                   long validPermissionEntries, long skippedPermissionEntries,
                                   long errorCount) {
        this.totalSourceRows = totalSourceRows;
        this.totalPermissionEntries = totalPermissionEntries;
        this.validPermissionEntries = validPermissionEntries;
        this.skippedPermissionEntries = skippedPermissionEntries;
        this.errorCount = errorCount;
        this.parsedAt = Instant.now();
        this.validatedAt = this.parsedAt;
        this.status = errorCount == 0 ? ImportJobStatus.READY : ImportJobStatus.INVALID;
        this.failureMessage = null;
    }

    public void markParsingFailed(String message) {
        this.status = ImportJobStatus.FAILED;
        this.failureMessage = message;
        this.parsedAt = Instant.now();
    }

    public void startExecution() {
        this.status = ImportJobStatus.EXECUTING;
        this.executionStartedAt = Instant.now();
        this.failureMessage = null;
    }

    public void returnToReady(String message) {
        this.status = ImportJobStatus.READY;
        this.failureMessage = message;
    }

    public void completeExecution(long addCount, long updateCount, long removeCount,
                                  long skipCount, long failedCount) {
        this.addCount = addCount;
        this.updateCount = updateCount;
        this.removeCount = removeCount;
        this.skipCount = skipCount;
        this.failedCount = failedCount;
        this.status = failedCount == 0 ? ImportJobStatus.COMPLETED : ImportJobStatus.FAILED;
        this.executedAt = Instant.now();
    }

    public void markExecutionFailed(String message, long addCount, long updateCount, long removeCount,
                                    long skipCount, long failedCount) {
        this.addCount = addCount;
        this.updateCount = updateCount;
        this.removeCount = removeCount;
        this.skipCount = skipCount;
        this.failedCount = failedCount;
        this.status = ImportJobStatus.FAILED;
        this.failureMessage = message;
        this.executedAt = Instant.now();
    }

    public Long getId() { return id; }
    public long getVersion() { return version; }
    public String getJobType() { return jobType; }
    public String getFileName() { return fileName; }
    public String getFileChecksum() { return fileChecksum; }
    public String getStoragePath() { return storagePath; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public ImportJobStatus getStatus() { return status; }
    public long getTotalSourceRows() { return totalSourceRows; }
    public long getTotalPermissionEntries() { return totalPermissionEntries; }
    public long getValidPermissionEntries() { return validPermissionEntries; }
    public long getSkippedPermissionEntries() { return skippedPermissionEntries; }
    public long getErrorCount() { return errorCount; }
    public long getAddCount() { return addCount; }
    public long getUpdateCount() { return updateCount; }
    public long getRemoveCount() { return removeCount; }
    public long getSkipCount() { return skipCount; }
    public long getFailedCount() { return failedCount; }
    public Instant getParsedAt() { return parsedAt; }
    public Instant getValidatedAt() { return validatedAt; }
    public Instant getExecutionStartedAt() { return executionStartedAt; }
    public Instant getExecutedAt() { return executedAt; }
    public String getFailureMessage() { return failureMessage; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
