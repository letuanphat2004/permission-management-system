package com.windowauthorizer.permission.importjob.engine;

import com.windowauthorizer.permission.importjob.domain.PermissionLevel;

public record PermissionCommand(
        long sourceRowNumber,
        int aceIndex,
        String resourcePath,
        String principalName,
        PermissionLevel desiredPermission,
        boolean inheritedAce,
        boolean breaksInheritance
) {
}
