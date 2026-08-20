package com.windowauthorizer.permission.importjob.dto;

import com.windowauthorizer.permission.importjob.domain.ExecutionAction;
import com.windowauthorizer.permission.importjob.domain.ExecutionStatus;
import com.windowauthorizer.permission.importjob.domain.PermissionLevel;
import com.windowauthorizer.permission.importjob.entity.ImportExecutionResult;

public record ExecutionItemResponse(
        Long id,
        long rowNumber,
        Integer aceIndex,
        String resourcePath,
        String principalName,
        PermissionLevel previousPermission,
        PermissionLevel desiredPermission,
        ExecutionAction action,
        ExecutionStatus status,
        String engineRequestId,
        String errorCode,
        String message
) {
    public static ExecutionItemResponse from(ImportExecutionResult result) {
        return new ExecutionItemResponse(
                result.getId(), result.getSourceRowNumber(), result.getAceIndex(), result.getResourcePath(),
                result.getPrincipalName(), result.getPreviousPermission(), result.getDesiredPermission(),
                result.getAction(), result.getStatus(), result.getEngineRequestId(), result.getErrorCode(),
                result.getMessage()
        );
    }
}
