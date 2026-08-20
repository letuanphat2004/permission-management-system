package com.windowauthorizer.permission.importjob.dto;

import com.windowauthorizer.permission.importjob.entity.ImportError;

public record ImportErrorResponse(
        Long id,
        long rowNumber,
        String columnName,
        Integer aceIndex,
        String rawValue,
        String errorCode,
        String errorMessage,
        String suggestion
) {
    public static ImportErrorResponse from(ImportError error) {
        return new ImportErrorResponse(
                error.getId(), error.getSourceRowNumber(), error.getColumnName(), error.getAceIndex(),
                error.getRawValue(), error.getErrorCode(), error.getErrorMessage(), error.getSuggestion()
        );
    }
}
