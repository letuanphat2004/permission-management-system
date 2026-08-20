package com.windowauthorizer.permission.importjob.validation;

import com.windowauthorizer.permission.importjob.domain.PermissionLevel;
import com.windowauthorizer.permission.importjob.engine.PermissionCommand;
import com.windowauthorizer.permission.importjob.parser.ImportFileFormatException;
import com.windowauthorizer.permission.importjob.parser.ImportFileParser;
import com.windowauthorizer.permission.importjob.parser.RawImportRow;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class PermissionImportProcessor {
    private static final String COL_PATH = "Duong dan";
    private static final String COL_TYPE = "Loai";
    private static final String COL_ACE_COUNT = "So ACE";
    private static final String COL_INHERITANCE = "Ngat ke thua";
    private static final String COL_PERMISSIONS = "Ai co quyen gi";

    private final ImportFileParser parser;

    public PermissionImportProcessor(ImportFileParser parser) {
        this.parser = parser;
    }

    public ValidationReport scan(Path path, Consumer<ValidationIssue> issueConsumer,
                                 Consumer<PermissionCommand> commandConsumer) {
        Accumulator accumulator = new Accumulator(issueConsumer, commandConsumer);
        try {
            parser.parse(path, accumulator::accept);
        } catch (ImportFileFormatException exception) {
            accumulator.issue(new ValidationIssue(
                    exception.getRowNumber(), exception.getColumnName(), null, exception.getRawValue(),
                    exception.getErrorCode(), exception.getMessage(), "Kiểm tra lại cấu trúc file nguồn."
            ));
        }
        accumulator.ensureDataRowExists();
        return accumulator.report();
    }

    private static final class Accumulator {
        private final Consumer<ValidationIssue> issueConsumer;
        private final Consumer<PermissionCommand> commandConsumer;
        private final Map<String, SeenPermission> seenPermissions = new HashMap<>();
        private long totalRows;
        private long totalEntries;
        private long validEntries;
        private long skippedEntries;
        private long errors;

        private Accumulator(Consumer<ValidationIssue> issueConsumer,
                            Consumer<PermissionCommand> commandConsumer) {
            this.issueConsumer = issueConsumer;
            this.commandConsumer = commandConsumer;
        }

        private void accept(RawImportRow row) {
            totalRows++;
            String path = normalizePath(row.path());
            boolean pathValid = validatePath(row, path);
            boolean typeValid = validateType(row);
            Boolean breaksInheritance = parseInheritance(row);

            String[] rawAces = splitAces(row.permissions());
            int actualAceCount = rawAces.length;
            Integer declaredAceCount = parseAceCount(row);
            if (declaredAceCount != null && declaredAceCount != actualAceCount) {
                issue(new ValidationIssue(row.rowNumber(), COL_ACE_COUNT, null, row.aceCount(),
                        "ACE_COUNT_MISMATCH",
                        "Số ACE khai báo là " + declaredAceCount + " nhưng thực tế đọc được " + actualAceCount + ".",
                        "Xuất lại báo cáo hoặc sửa giá trị số ACE."));
            }

            if (actualAceCount == 0) {
                issue(new ValidationIssue(row.rowNumber(), COL_PERMISSIONS, null, row.permissions(),
                        "EMPTY_PERMISSION_LIST", "Dòng không có thông tin phân quyền.",
                        "Bổ sung ít nhất một principal và quyền."));
                return;
            }

            for (int index = 0; index < rawAces.length; index++) {
                totalEntries++;
                int aceIndex = index + 1;
                ParsedAce ace = parseAce(row.rowNumber(), aceIndex, rawAces[index]);
                if (ace != null && ace.skipped()) {
                    skippedEntries++;
                    continue;
                }
                boolean valid = pathValid && typeValid && breaksInheritance != null && ace != null;
                if (valid) {
                    String duplicateKey = path.toUpperCase(Locale.ROOT) + '\u0000'
                            + ace.principalName().toUpperCase(Locale.ROOT);
                    SeenPermission previous = seenPermissions.putIfAbsent(duplicateKey,
                            new SeenPermission(row.rowNumber(), ace.permission()));
                    if (previous != null) {
                        if (previous.permission() == ace.permission()) {
                            skippedEntries++;
                        } else {
                            issue(new ValidationIssue(row.rowNumber(), COL_PERMISSIONS, aceIndex, rawAces[index],
                                    "CONFLICTING_PERMISSION",
                                    "Quyền xung đột với dòng " + previous.rowNumber() + " ("
                                            + previous.permission() + ").",
                                    "Chỉ giữ một quyền cho cùng path và principal."));
                        }
                        valid = false;
                    }
                }

                if (valid) {
                    validEntries++;
                    commandConsumer.accept(new PermissionCommand(
                            row.rowNumber(), aceIndex, path, ace.principalName(), ace.permission(),
                            ace.inherited(), breaksInheritance
                    ));
                }
            }
        }

        private boolean validatePath(RawImportRow row, String path) {
            if (path.isBlank()) {
                issue(new ValidationIssue(row.rowNumber(), COL_PATH, null, row.path(), "PATH_REQUIRED",
                        "Đường dẫn không được để trống.", "Nhập đường dẫn thư mục."));
                return false;
            }
            if (path.length() > 2048 || path.indexOf('\0') >= 0) {
                issue(new ValidationIssue(row.rowNumber(), COL_PATH, null, row.path(), "INVALID_PATH",
                        "Đường dẫn không hợp lệ hoặc quá dài.", "Kiểm tra lại đường dẫn thư mục."));
                return false;
            }
            return true;
        }

        private boolean validateType(RawImportRow row) {
            if (!normalizeToken(row.type()).equals("DIRECTORY")) {
                issue(new ValidationIssue(row.rowNumber(), COL_TYPE, null, row.type(), "UNSUPPORTED_RESOURCE_TYPE",
                        "Phiên bản hiện tại chỉ hỗ trợ Directory.", "Đặt loại tài nguyên thành Directory."));
                return false;
            }
            return true;
        }

        private Integer parseAceCount(RawImportRow row) {
            try {
                int count = Integer.parseInt(value(row.aceCount()));
                if (count < 0) {
                    throw new NumberFormatException("negative");
                }
                return count;
            } catch (NumberFormatException exception) {
                issue(new ValidationIssue(row.rowNumber(), COL_ACE_COUNT, null, row.aceCount(),
                        "INVALID_ACE_COUNT", "Số ACE phải là số nguyên không âm.", "Nhập lại số ACE hợp lệ."));
                return null;
            }
        }

        private Boolean parseInheritance(RawImportRow row) {
            return switch (normalizeToken(row.breaksInheritance())) {
                case "CO", "YES", "TRUE" -> true;
                case "KHONG", "NO", "FALSE" -> false;
                default -> {
                    issue(new ValidationIssue(row.rowNumber(), COL_INHERITANCE, null, row.breaksInheritance(),
                            "INVALID_INHERITANCE_VALUE", "Giá trị ngắt kế thừa phải là Co hoặc Khong.",
                            "Sử dụng Co hoặc Khong."));
                    yield null;
                }
            };
        }

        private ParsedAce parseAce(long rowNumber, int aceIndex, String rawAce) {
            String aceText = value(rawAce);
            boolean deny = normalizeToken(aceText).startsWith("TU CHOI ") || aceText.startsWith("[TU CHOI]");
            if (deny) {
                return ParsedAce.skip();
            }

            int separator = aceText.indexOf(':');
            if (separator <= 0 || separator == aceText.length() - 1) {
                issue(new ValidationIssue(rowNumber, COL_PERMISSIONS, aceIndex, rawAce, "INVALID_ACE_FORMAT",
                        "ACE phải có dạng DOMAIN\\principal: permission.",
                        "Kiểm tra lại principal và quyền, ngăn cách bằng dấu hai chấm."));
                return null;
            }

            String principal = aceText.substring(0, separator).trim()
                    .replaceFirst("\\s+\\([^()]*\\)\\s*$", "")
                    .trim();
            if (principal.isBlank() || principal.length() > 512) {
                issue(new ValidationIssue(rowNumber, COL_PERMISSIONS, aceIndex, rawAce, "INVALID_PRINCIPAL",
                        "Tên user/group không hợp lệ hoặc quá dài.", "Kiểm tra lại tên user/group trong AD."));
                return null;
            }

            String permissionText = aceText.substring(separator + 1).trim();
            boolean inherited = permissionText.toLowerCase(Locale.ROOT).matches(".*\\(thua ke\\)\\s*$");
            permissionText = permissionText.replaceFirst("(?i)\\s*\\(thua ke\\)\\s*$", "").trim();
            PermissionLevel permission = parsePermission(permissionText);
            if (permission == null) {
                issue(new ValidationIssue(rowNumber, COL_PERMISSIONS, aceIndex, rawAce, "UNSUPPORTED_PERMISSION",
                        "Quyền '" + permissionText + "' chưa được hỗ trợ.",
                        "Chỉ sử dụng NONE, READ, MODIFY hoặc FULL_CONTROL."));
                return null;
            }
            return new ParsedAce(principal, permission, inherited);
        }

        private PermissionLevel parsePermission(String rawPermission) {
            return switch (normalizeToken(rawPermission)) {
                case "NONE" -> PermissionLevel.NONE;
                case "READ", "DOC", "DOC VA CHAY", "READ EXECUTE", "READ AND EXECUTE" -> PermissionLevel.READ;
                case "MODIFY", "SUA" -> PermissionLevel.MODIFY;
                case "FULL CONTROL", "FULLCONTROL", "TOAN QUYEN" -> PermissionLevel.FULL_CONTROL;
                case "SPECIAL PERMISSION", "SPECIAL PERMISSIONS", "QUYEN DAC BIET" -> PermissionLevel.SPECIAL_PERMISSION;
                default -> null;
            };
        }

        private void issue(ValidationIssue issue) {
            errors++;
            issueConsumer.accept(issue);
        }

        private ValidationReport report() {
            return new ValidationReport(totalRows, totalEntries, validEntries, skippedEntries, errors);
        }

        private void ensureDataRowExists() {
            if (totalRows == 0 && errors == 0) {
                issue(new ValidationIssue(2, "ROW", null, null, "EMPTY_DATA",
                        "File không có dòng dữ liệu để import.", "Bổ sung dữ liệu và import lại file."));
            }
        }

        private String[] splitAces(String permissions) {
            String value = value(permissions);
            if (value.isBlank()) {
                return new String[0];
            }
            return java.util.Arrays.stream(value.split(";"))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .toArray(String[]::new);
        }

        private String normalizePath(String rawPath) {
            return value(rawPath).replace('/', '\\');
        }

        private String normalizeToken(String input) {
            String normalized = Normalizer.normalize(value(input), Normalizer.Form.NFD)
                    .replace("Đ", "D").replace("đ", "d")
                    .replaceAll("\\p{M}", "")
                    .replaceAll("[^A-Za-z0-9]+", " ")
                    .trim()
                    .replaceAll("\\s+", " ");
            return normalized.toUpperCase(Locale.ROOT);
        }

        private String value(String input) {
            return input == null ? "" : input.trim();
        }
    }

    private record ParsedAce(String principalName, PermissionLevel permission, boolean inherited, boolean skipped) {
        private ParsedAce(String principalName, PermissionLevel permission, boolean inherited) {
            this(principalName, permission, inherited, false);
        }

        private static ParsedAce skip() {
            return new ParsedAce("", null, false, true);
        }
    }

    private record SeenPermission(long rowNumber, PermissionLevel permission) {
    }
}
