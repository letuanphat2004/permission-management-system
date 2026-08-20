package com.windowauthorizer.permission.importjob.parser;

public record RawImportRow(
        long rowNumber,
        String path,
        String type,
        String aceCount,
        String breaksInheritance,
        String permissions
) {
}
