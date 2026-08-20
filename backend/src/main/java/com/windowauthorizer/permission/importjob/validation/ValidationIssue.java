package com.windowauthorizer.permission.importjob.validation;

public record ValidationIssue(
        long rowNumber,
        String columnName,
        Integer aceIndex,
        String rawValue,
        String errorCode,
        String errorMessage,
        String suggestion
) {
}
