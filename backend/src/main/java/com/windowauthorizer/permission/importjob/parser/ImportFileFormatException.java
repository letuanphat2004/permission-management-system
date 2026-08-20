package com.windowauthorizer.permission.importjob.parser;

public class ImportFileFormatException extends RuntimeException {
    private final long rowNumber;
    private final String columnName;
    private final String errorCode;
    private final String rawValue;

    public ImportFileFormatException(long rowNumber, String columnName, String errorCode,
                                     String rawValue, String message, Throwable cause) {
        super(message, cause);
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.errorCode = errorCode;
        this.rawValue = rawValue;
    }

    public ImportFileFormatException(long rowNumber, String columnName, String errorCode,
                                     String rawValue, String message) {
        this(rowNumber, columnName, errorCode, rawValue, message, null);
    }

    public long getRowNumber() { return rowNumber; }
    public String getColumnName() { return columnName; }
    public String getErrorCode() { return errorCode; }
    public String getRawValue() { return rawValue; }
}
