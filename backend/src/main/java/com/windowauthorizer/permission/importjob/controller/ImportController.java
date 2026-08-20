package com.windowauthorizer.permission.importjob.controller;

import com.windowauthorizer.permission.common.api.PageResponse;
import com.windowauthorizer.permission.importjob.dto.ImportErrorResponse;
import com.windowauthorizer.permission.importjob.dto.ImportJobResponse;
import com.windowauthorizer.permission.importjob.dto.ImportResultResponse;
import com.windowauthorizer.permission.importjob.service.ImportExecutionService;
import com.windowauthorizer.permission.importjob.service.ImportJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
public class ImportController {
    private final ImportJobService importJobService;
    private final ImportExecutionService executionService;

    public ImportController(ImportJobService importJobService, ImportExecutionService executionService) {
        this.importJobService = importJobService;
        this.executionService = executionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportJobResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return importJobService.uploadAndValidate(file, actor);
    }

    @GetMapping("/{id}")
    public ImportJobResponse get(@PathVariable Long id) {
        return importJobService.get(id);
    }

    @GetMapping("/{id}/errors")
    public PageResponse<ImportErrorResponse> errors(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return importJobService.getErrors(id, page, size);
    }

    @PostMapping("/{id}/execute")
    public ImportJobResponse execute(@PathVariable Long id) {
        return executionService.execute(id);
    }

    @GetMapping("/{id}/result")
    public ImportResultResponse result(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return importJobService.getResult(id, page, size);
    }
}
