package com.windowauthorizer.permission.importjob.service;

import com.windowauthorizer.permission.common.exception.ApiException;
import com.windowauthorizer.permission.importjob.entity.ImportError;
import com.windowauthorizer.permission.importjob.entity.ImportExecutionResult;
import com.windowauthorizer.permission.importjob.entity.ImportJob;
import com.windowauthorizer.permission.importjob.engine.EngineCommandResult;
import com.windowauthorizer.permission.importjob.repository.ImportErrorRepository;
import com.windowauthorizer.permission.importjob.repository.ImportExecutionResultRepository;
import com.windowauthorizer.permission.importjob.repository.ImportJobRepository;
import com.windowauthorizer.permission.importjob.validation.ValidationIssue;
import com.windowauthorizer.permission.importjob.validation.ValidationReport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ImportJobPersistenceService {
    private final ImportJobRepository jobRepository;
    private final ImportErrorRepository errorRepository;
    private final ImportExecutionResultRepository resultRepository;

    public ImportJobPersistenceService(ImportJobRepository jobRepository,
                                       ImportErrorRepository errorRepository,
                                       ImportExecutionResultRepository resultRepository) {
        this.jobRepository = jobRepository;
        this.errorRepository = errorRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional
    public ImportJob create(ImportJob job) {
        return jobRepository.saveAndFlush(job);
    }

    @Transactional
    public void saveErrors(Long jobId, List<ValidationIssue> issues) {
        if (issues.isEmpty()) {
            return;
        }
        ImportJob job = jobRepository.getReferenceById(jobId);
        List<ImportError> errors = issues.stream()
                .map(issue -> new ImportError(job, issue.rowNumber(), issue.columnName(), issue.aceIndex(),
                        issue.rawValue(), issue.errorCode(), issue.errorMessage(), issue.suggestion()))
                .toList();
        errorRepository.saveAll(errors);
        errorRepository.flush();
    }

    @Transactional
    public ImportJob completeValidation(Long jobId, ValidationReport report) {
        ImportJob job = find(jobId);
        job.completeValidation(report.totalSourceRows(), report.totalPermissionEntries(),
                report.validPermissionEntries(), report.skippedPermissionEntries(), report.errorCount());
        return jobRepository.saveAndFlush(job);
    }

    @Transactional
    public void markParsingFailed(Long jobId, String message) {
        ImportJob job = find(jobId);
        job.markParsingFailed(message);
        jobRepository.save(job);
    }

    @Transactional
    public ImportJob startExecution(Long jobId) {
        ImportJob job = find(jobId);
        if (job.getStatus() != com.windowauthorizer.permission.importjob.domain.ImportJobStatus.READY
                || job.getErrorCount() != 0) {
            throw new ApiException(HttpStatus.CONFLICT, "IMPORT_NOT_READY",
                    "Chỉ có thể execute một file đã validate thành công và không có lỗi.");
        }
        job.startExecution();
        return jobRepository.saveAndFlush(job);
    }

    @Transactional
    public void returnToReady(Long jobId, String message) {
        ImportJob job = find(jobId);
        job.returnToReady(message);
        jobRepository.save(job);
    }

    @Transactional
    public void saveExecutionResults(Long jobId, List<EngineCommandResult> engineResults) {
        if (engineResults.isEmpty()) {
            return;
        }
        ImportJob job = jobRepository.getReferenceById(jobId);
        List<ImportExecutionResult> results = engineResults.stream()
                .map(result -> new ImportExecutionResult(
                        job,
                        result.command().sourceRowNumber(),
                        result.command().aceIndex(),
                        result.command().resourcePath(),
                        result.command().principalName(),
                        result.previousPermission(),
                        result.command().desiredPermission(),
                        result.action(),
                        result.success()
                                ? com.windowauthorizer.permission.importjob.domain.ExecutionStatus.SUCCESS
                                : com.windowauthorizer.permission.importjob.domain.ExecutionStatus.FAILED,
                        result.engineRequestId(), result.errorCode(), result.message()
                ))
                .toList();
        resultRepository.saveAll(results);
        resultRepository.flush();
    }

    @Transactional
    public ImportJob completeExecution(Long jobId, ExecutionCounters counters) {
        ImportJob job = find(jobId);
        job.completeExecution(counters.add(), counters.update(), counters.remove(), counters.skip(), counters.failed());
        return jobRepository.saveAndFlush(job);
    }

    @Transactional
    public void markExecutionFailed(Long jobId, String message, ExecutionCounters counters) {
        ImportJob job = find(jobId);
        job.markExecutionFailed(message, counters.add(), counters.update(), counters.remove(),
                counters.skip(), counters.failed());
        jobRepository.save(job);
    }

    private ImportJob find(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IMPORT_NOT_FOUND",
                        "Không tìm thấy import job " + id + "."));
    }
}
