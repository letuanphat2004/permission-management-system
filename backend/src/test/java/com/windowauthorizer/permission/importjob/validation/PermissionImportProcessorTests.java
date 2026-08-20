package com.windowauthorizer.permission.importjob.validation;

import com.windowauthorizer.permission.importjob.domain.PermissionLevel;
import com.windowauthorizer.permission.importjob.parser.ImportFileParser;
import com.windowauthorizer.permission.importjob.parser.RawImportRow;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionImportProcessorTests {

    @Test
    void mapsSupportedVietnamesePermissionsToCanonicalLevels() {
        ImportFileParser parser = parserOf(new RawImportRow(
                2, "\\Finance", "Directory", "2", "Khong",
                "FRA\\P.KeToan: Sua; FRA\\P.KyThuat: Doc va chay (thua ke)"
        ));
        PermissionImportProcessor processor = new PermissionImportProcessor(parser);
        var errors = new ArrayList<ValidationIssue>();
        var commands = new ArrayList<com.windowauthorizer.permission.importjob.engine.PermissionCommand>();

        ValidationReport report = processor.scan(Path.of("ignored.csv"), errors::add, commands::add);

        assertThat(report.errorCount()).isZero();
        assertThat(report.totalSourceRows()).isEqualTo(1);
        assertThat(report.totalPermissionEntries()).isEqualTo(2);
        assertThat(commands).extracting(command -> command.desiredPermission())
                .containsExactly(PermissionLevel.MODIFY, PermissionLevel.READ);
        assertThat(commands.get(1).inheritedAce()).isTrue();
    }

    @Test
    void skipsDenyAndMapsWindowsPermissionNames() {
        ImportFileParser parser = parserOf(new RawImportRow(
                8, "\\Finance", "Directory", "3", "Co",
                "[TU CHOI] FRA\\dungnh: Toan quyen; FRA\\Administrator: Toan quyen; FRA\\duynt: Quyen dac biet"
        ));
        PermissionImportProcessor processor = new PermissionImportProcessor(parser);
        var errors = new ArrayList<ValidationIssue>();
        var commands = new ArrayList<com.windowauthorizer.permission.importjob.engine.PermissionCommand>();

        ValidationReport report = processor.scan(Path.of("ignored.csv"), errors::add, commands::add);

        assertThat(report.errorCount()).isZero();
        assertThat(report.skippedPermissionEntries()).isEqualTo(1);
        assertThat(errors).isEmpty();
        assertThat(commands).extracting(command -> command.desiredPermission())
                .containsExactly(PermissionLevel.FULL_CONTROL, PermissionLevel.SPECIAL_PERMISSION);
    }

    @Test
    void detectsConflictingPermissionsForSamePathAndPrincipal() {
        ImportFileParser parser = (path, consumer) -> {
            consumer.accept(new RawImportRow(2, "\\Finance", "Directory", "1", "Khong",
                    "FRA\\P.KeToan: Doc va chay"));
            consumer.accept(new RawImportRow(3, "\\Finance", "Directory", "1", "Khong",
                    "FRA\\P.KeToan: Sua"));
        };
        PermissionImportProcessor processor = new PermissionImportProcessor(parser);
        var errors = new ArrayList<ValidationIssue>();

        processor.scan(Path.of("ignored.csv"), errors::add, command -> { });

        assertThat(errors).extracting(ValidationIssue::errorCode)
                .containsExactly("CONFLICTING_PERMISSION");
        assertThat(errors.getFirst().rowNumber()).isEqualTo(3);
    }

    @Test
    void mergesSamePermissionForExplicitAndInheritedAce() {
        ImportFileParser parser = parserOf(new RawImportRow(
                1586, "\\1.14 HS Thu nghiem", "Directory", "2", "Khong",
                "FRA\\sys.fssm (System MS-FSSM): Toan quyen; "
                        + "FRA\\sys.fssm (System MS-FSSM): Toan quyen (thua ke)"
        ));
        PermissionImportProcessor processor = new PermissionImportProcessor(parser);
        var errors = new ArrayList<ValidationIssue>();
        var commands = new ArrayList<com.windowauthorizer.permission.importjob.engine.PermissionCommand>();

        ValidationReport report = processor.scan(Path.of("ignored.csv"), errors::add, commands::add);

        assertThat(report.errorCount()).isZero();
        assertThat(report.validPermissionEntries()).isEqualTo(1);
        assertThat(report.skippedPermissionEntries()).isEqualTo(1);
        assertThat(commands).hasSize(1);
        assertThat(commands.getFirst().inheritedAce()).isFalse();
    }

    private ImportFileParser parserOf(RawImportRow row) {
        return (path, consumer) -> consumer.accept(row);
    }
}
