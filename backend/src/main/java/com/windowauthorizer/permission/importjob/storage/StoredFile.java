package com.windowauthorizer.permission.importjob.storage;

public record StoredFile(
        String originalFileName,
        String storagePath,
        String checksum,
        long sizeBytes
) {
}
