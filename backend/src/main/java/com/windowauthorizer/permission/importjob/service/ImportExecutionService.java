package com.windowauthorizer.permission.importjob.service;

import com.windowauthorizer.permission.common.exception.ApiException;
import com.windowauthorizer.permission.importjob.dto.ImportJobResponse;
import com.windowauthorizer.permission.importjob.domain.ImportJobStatus;
import com.windowauthorizer.permission.importjob.engine.EngineCommandResult;
import com.windowauthorizer.permission.importjob.engine.EngineUnavailableException;
import com.windowauthorizer.permission.importjob.engine.PermissionCommand;
import com.windowauthorizer.permission.importjob.engine.PermissionEngineClient;
import com.windowauthorizer.permission.importjob.entity.ImportJob;
import com.windowauthorizer.permission.importjob.storage.ImportFileStorage;
import com.windowauthorizer.permission.importjob.validation.PermissionImportProcessor;
import com.windowauthorizer.permission.importjob.validation.ValidationReport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportExecutionService {
    private static final int ENGINE_BATCH_SIZE = 100;

    private final ImportJobService importJobService;
    private final ImportJobPersistenceService persistenceService;
    private final ImportFileStorage fileStorage;
    private final PermissionImportProcessor processor;
    private final PermissionEngineClient engineClient;

    public ImportExecutionService(ImportJobService importJobService,
                                  ImportJobPersistenceService persistenceService,
                                  ImportFileStorage fileStorage,
                                  PermissionImportProcessor processor,
                                  PermissionEngineClient engineClient) {
        this.importJobService = importJobService;
        this.persistenceService = persistenceService;
        this.fileStorage = fileStorage;
        this.processor = processor;
        this.engineClient = engineClient;
    }

    public ImportJobResponse execute(Long jobId) {
        ImportJob current = importJobService.findJob(jobId);
        if (current.getStatus() != ImportJobStatus.READY || current.getErrorCount() != 0) {
            throw new ApiException(HttpStatus.CONFLICT, "IMPORT_NOT_READY",
                    "Chỉ có thể execute một file đã validate thành công và không có lỗi.");
        }
        if (!engineClient.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ENGINE_NOT_CONFIGURED",
                    "Permission Engine chưa được cấu hình; backend sẽ không giả lập thay đổi AD.");
        }

        Path source = fileStorage.resolve(current.getStoragePath());
        if (!fileStorage.checksum(source).equals(current.getFileChecksum())) {
            throw new ApiException(HttpStatus.CONFLICT, "FILE_CHECKSUM_MISMATCH",
                    "File nguồn đã thay đổi sau khi validate. Vui lòng import lại.");
        }

        ValidationReport revalidation = processor.scan(source, issue -> { }, command -> { });
        if (!revalidation.valid()) {
            throw new ApiException(HttpStatus.CONFLICT, "FILE_REVALIDATION_FAILED",
                    "File không còn hợp lệ. Vui lòng import lại file.");
        }

        persistenceService.startExecution(jobId);
        ExecutionBuffer buffer = new ExecutionBuffer(jobId);
        try {
            ValidationReport executionScan = processor.scan(source, issue -> { }, buffer::add);
            if (!executionScan.valid()) {
                throw new IllegalStateException("File phát sinh lỗi trong lúc tạo command.");
            }
            buffer.flush();
            ImportJob completed = persistenceService.completeExecution(jobId, buffer.counters);
            return ImportJobResponse.from(completed);
        } catch (EngineUnavailableException exception) {
            if (buffer.counters.processed() == 0) {
                persistenceService.returnToReady(jobId, exception.getMessage());
            } else {
                persistenceService.markExecutionFailed(jobId, exception.getMessage(), buffer.counters);
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ENGINE_UNAVAILABLE", exception.getMessage());
        } catch (RuntimeException exception) {
            persistenceService.markExecutionFailed(jobId,
                    exception.getMessage() == null ? "Execute thất bại." : exception.getMessage(), buffer.counters);
            throw exception;
        }
    }

    private final class ExecutionBuffer {
        private final Long jobId;
        private final List<PermissionCommand> commands = new ArrayList<>(ENGINE_BATCH_SIZE);
        private ExecutionCounters counters = ExecutionCounters.empty();

        private ExecutionBuffer(Long jobId) {
            this.jobId = jobId;
        }

        private void add(PermissionCommand command) {
            commands.add(command);
            if (commands.size() >= ENGINE_BATCH_SIZE) {
                flush();
            }
        }

        private void flush() {
            if (commands.isEmpty()) {
                return;
            }
            List<PermissionCommand> batch = List.copyOf(commands);
            List<EngineCommandResult> results = engineClient.execute(batch);
            if (results == null || results.size() != batch.size()) {
                throw new EngineUnavailableException("Permission Engine trả về số lượng kết quả không khớp command.");
            }
            persistenceService.saveExecutionResults(jobId, results);
            counters = counters.addAll(results);
            commands.clear();
        }
    }
}
