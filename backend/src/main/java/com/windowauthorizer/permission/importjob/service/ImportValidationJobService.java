package com.windowauthorizer.permission.importjob.service;

import com.windowauthorizer.permission.importjob.storage.ImportFileStorage;
import com.windowauthorizer.permission.importjob.validation.PermissionImportProcessor;
import com.windowauthorizer.permission.importjob.validation.ValidationIssue;
import com.windowauthorizer.permission.importjob.validation.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImportValidationJobService {
    private static final Logger log = LoggerFactory.getLogger(ImportValidationJobService.class);
    private static final int ERROR_BATCH_SIZE = 500;

    private final ImportFileStorage fileStorage;
    private final PermissionImportProcessor processor;
    private final ImportJobPersistenceService persistenceService;

    public ImportValidationJobService(ImportFileStorage fileStorage,
                                      PermissionImportProcessor processor,
                                      ImportJobPersistenceService persistenceService) {
        this.fileStorage = fileStorage;
        this.processor = processor;
        this.persistenceService = persistenceService;
    }

    @Async("importTaskExecutor")
    public void validate(Long jobId, String storagePath) {
        try {
            ErrorBuffer errorBuffer = new ErrorBuffer(jobId);
            ValidationReport report = processor.scan(
                    fileStorage.resolve(storagePath), errorBuffer::add, command -> { }
            );
            errorBuffer.flush();
            persistenceService.completeValidation(jobId, report);
        } catch (RuntimeException exception) {
            log.error("Import job {} failed while parsing", jobId, exception);
            persistenceService.markParsingFailed(jobId, safeMessage(exception));
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Không thể parse file import." : message;
    }

    private final class ErrorBuffer {
        private final Long jobId;
        private final List<ValidationIssue> issues = new ArrayList<>(ERROR_BATCH_SIZE);

        private ErrorBuffer(Long jobId) {
            this.jobId = jobId;
        }

        private void add(ValidationIssue issue) {
            issues.add(issue);
            if (issues.size() >= ERROR_BATCH_SIZE) {
                flush();
            }
        }

        private void flush() {
            if (issues.isEmpty()) {
                return;
            }
            persistenceService.saveErrors(jobId, List.copyOf(issues));
            issues.clear();
        }
    }
}
