package com.windowauthorizer.permission.importjob.engine;

import com.windowauthorizer.permission.importjob.domain.ExecutionAction;
import com.windowauthorizer.permission.importjob.domain.PermissionLevel;

public record EngineCommandResult(
        PermissionCommand command,
        PermissionLevel previousPermission,
        ExecutionAction action,
        boolean success,
        String engineRequestId,
        String errorCode,
        String message
) {
}
