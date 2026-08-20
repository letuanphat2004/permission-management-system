package com.windowauthorizer.permission.importjob.service;

import com.windowauthorizer.permission.common.api.PageResponse;
import com.windowauthorizer.permission.common.exception.ApiException;
import com.windowauthorizer.permission.importjob.dto.ExecutionItemResponse;
import com.windowauthorizer.permission.importjob.dto.ImportErrorResponse;
import com.windowauthorizer.permission.importjob.dto.ImportJobResponse;
import com.windowauthorizer.permission.importjob.dto.ImportResultResponse;
import com.windowauthorizer.permission.importjob.entity.ImportJob;
import com.windowauthorizer.permission.importjob.repository.ImportErrorRepository;
import com.windowauthorizer.permission.importjob.repository.ImportExecutionResultRepository;
import com.windowauthorizer.permission.importjob.repository.ImportJobRepository;
import com.windowauthorizer.permission.importjob.storage.ImportFileStorage;
import com.windowauthorizer.permission.importjob.storage.StoredFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportJobService {
    private final ImportFileStorage fileStorage;
    private final ImportJobPersistenceService persistenceService;
    private final ImportValidationJobService validationJobService;
    private final ImportJobRepository jobRepository;
    private final ImportErrorRepository errorRepository;
    private final ImportExecutionResultRepository resultRepository;

    public ImportJobService(ImportFileStorage fileStorage,
                            ImportJobPersistenceService persistenceService,
                            ImportValidationJobService validationJobService,
                            ImportJobRepository jobRepository,
                            ImportErrorRepository errorRepository,
                            ImportExecutionResultRepository resultRepository) {
        this.fileStorage = fileStorage;
        this.persistenceService = persistenceService;
        this.validationJobService = validationJobService;
        this.jobRepository = jobRepository;
        this.errorRepository = errorRepository;
        this.resultRepository = resultRepository;
    }

    public ImportJobResponse uploadAndValidate(MultipartFile file, String actor) {
        StoredFile storedFile = fileStorage.store(file);
        ImportJob job;
        try {
            job = persistenceService.create(ImportJob.parsing(
                    storedFile.originalFileName(), storedFile.checksum(), storedFile.storagePath(),
                    storedFile.sizeBytes(), normalizeActor(actor)
            ));
        } catch (RuntimeException exception) {
            fileStorage.deleteQuietly(storedFile.storagePath());
            throw exception;
        }

        try {
            validationJobService.validate(job.getId(), storedFile.storagePath());
        } catch (TaskRejectedException exception) {
            persistenceService.markParsingFailed(job.getId(), "Hàng đợi import đang đầy.");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "IMPORT_QUEUE_FULL",
                    "Hệ thống đang xử lý quá nhiều file import. Vui lòng thử lại sau.");
        }
        return ImportJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public ImportJobResponse get(Long id) {
        return ImportJobResponse.from(findJob(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ImportErrorResponse> getErrors(Long id, int page, int size) {
        findJob(id);
        var pageable = PageRequest.of(normalizePage(page), normalizeSize(size),
                Sort.by("sourceRowNumber").ascending().and(Sort.by("aceIndex").ascending())
                        .and(Sort.by("id").ascending()));
        return PageResponse.from(errorRepository.findByImportJobId(id, pageable), ImportErrorResponse::from);
    }

    @Transactional(readOnly = true)
    public ImportResultResponse getResult(Long id, int page, int size) {
        ImportJob job = findJob(id);
        var pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by("id").ascending());
        var items = PageResponse.from(resultRepository.findByImportJobId(id, pageable), ExecutionItemResponse::from);
        return new ImportResultResponse(job.getId(), job.getStatus(), job.getAddCount(), job.getUpdateCount(),
                job.getRemoveCount(), job.getSkipCount(), job.getFailedCount(), items);
    }

    ImportJob findJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IMPORT_NOT_FOUND",
                        "Không tìm thấy import job " + id + "."));
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 200);
    }

    private String normalizeActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "anonymous";
        }
        String value = actor.trim();
        return value.length() > 255 ? value.substring(0, 255) : value;
    }

}
