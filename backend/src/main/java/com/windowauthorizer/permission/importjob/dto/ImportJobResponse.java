package com.windowauthorizer.permission.importjob.dto;

import com.windowauthorizer.permission.importjob.domain.ImportJobStatus;
import com.windowauthorizer.permission.importjob.entity.ImportJob;

import java.time.Instant;

public record ImportJobResponse(
        Long id,
        String fileName,
        long fileSizeBytes,
        ImportJobStatus status,
        long totalSourceRows,
        long totalPermissionEntries,
        long validPermissionEntries,
        long skippedPermissionEntries,
        long errorCount,
        boolean executable,
        String failureMessage,
        String createdBy,
        Instant createdAt,
        Instant validatedAt,
        Instant executedAt
) {
    public static ImportJobResponse from(ImportJob job) {
        return new ImportJobResponse(
                job.getId(), job.getFileName(), job.getFileSizeBytes(), job.getStatus(),
                job.getTotalSourceRows(), job.getTotalPermissionEntries(),
                job.getValidPermissionEntries(), job.getSkippedPermissionEntries(), job.getErrorCount(),
                job.getStatus() == ImportJobStatus.READY && job.getErrorCount() == 0,
                job.getFailureMessage(), job.getCreatedBy(), job.getCreatedAt(),
                job.getValidatedAt(), job.getExecutedAt()
        );
    }
}
