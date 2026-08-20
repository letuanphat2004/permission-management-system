package com.windowauthorizer.permission.importjob.storage;

import com.windowauthorizer.permission.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ImportFileStorage {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "xls", "xlsx");
    private final Path uploadRoot;

    public ImportFileStorage(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Vui lòng chọn file CSV hoặc Excel.");
        }

        String originalName = sanitizeFileName(multipartFile.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE",
                    "Chỉ hỗ trợ file .csv, .xls hoặc .xlsx.");
        }

        Path jobDirectory = uploadRoot.resolve(UUID.randomUUID().toString()).normalize();
        Path destination = jobDirectory.resolve("source." + extension).normalize();
        ensureInsideUploadRoot(destination);

        try {
            Files.createDirectories(jobDirectory);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(multipartFile.getInputStream(), digest);
                 OutputStream output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
                input.transferTo(output);
            }
            return new StoredFile(originalName, destination.toString(),
                    HexFormat.of().formatHex(digest.digest()), Files.size(destination));
        } catch (IOException | NoSuchAlgorithmException exception) {
            deleteQuietly(destination);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORAGE_FAILED",
                    "Không thể lưu file import.");
        }
    }

    public Path resolve(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new ApiException(HttpStatus.CONFLICT, "IMPORT_FILE_MISSING",
                    "Import job không có thông tin file nguồn.");
        }
        Path path = Path.of(storagePath).toAbsolutePath().normalize();
        ensureInsideUploadRoot(path);
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.CONFLICT, "IMPORT_FILE_MISSING",
                    "File nguồn của lần import không còn tồn tại.");
        }
        return path;
    }

    public String checksum(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_CHECKSUM_FAILED",
                    "Không thể kiểm tra file nguồn của lần import.");
        }
    }

    public void deleteQuietly(String storagePath) {
        if (storagePath != null) {
            deleteQuietly(Path.of(storagePath));
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Path normalized = file.toAbsolutePath().normalize();
            ensureInsideUploadRoot(normalized);
            Files.deleteIfExists(normalized);
            Path parent = normalized.getParent();
            if (parent != null && !parent.equals(uploadRoot)) {
                Files.deleteIfExists(parent);
            }
        } catch (IOException ignored) {
            // Cleanup failure must not hide the original request failure.
        }
    }

    private void ensureInsideUploadRoot(Path path) {
        if (!path.startsWith(uploadRoot)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STORAGE_PATH", "Đường dẫn lưu file không hợp lệ.");
        }
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "import.csv";
        }
        String normalized = originalName.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return name.length() > 512 ? name.substring(name.length() - 512) : name;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
