package com.windowauthorizer.permission.importjob.validation;

public record ValidationReport(
        long totalSourceRows,
        long totalPermissionEntries,
        long validPermissionEntries,
        long skippedPermissionEntries,
        long errorCount
) {
    public boolean valid() {
        return errorCount == 0;
    }
}
