package com.windowauthorizer.permission.importjob.dto;

import com.windowauthorizer.permission.common.api.PageResponse;
import com.windowauthorizer.permission.importjob.domain.ImportJobStatus;

public record ImportResultResponse(
        Long importId,
        ImportJobStatus status,
        long addCount,
        long updateCount,
        long removeCount,
        long skipCount,
        long failedCount,
        PageResponse<ExecutionItemResponse> items
) {
}
