package com.windowauthorizer.permission.importjob.parser;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class ImportFileParserTests {

    @Test
    void readsCustomerCsvColumnsByPosition() throws Exception {
        Path fixture = Path.of(getClass().getResource("/fixtures/valid-import.csv").toURI());
        var rows = new ArrayList<RawImportRow>();

        new CsvImportFileParser().parse(fixture, rows::add);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().rowNumber()).isEqualTo(2);
        assertThat(rows.getFirst().path()).isEqualTo("\\Finance");
        assertThat(rows.getFirst().permissions()).contains("FRA\\P.KeToan: Sua");
    }

    @Test
    void readsFirstExcelSheet(@TempDir Path tempDirectory) throws Exception {
        Path workbookPath = tempDirectory.resolve("permission.xlsx");
        try (var workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(workbookPath)) {
            var sheet = workbook.createSheet("Permissions");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Duong dan");
            header.createCell(1).setCellValue("Loai");
            header.createCell(2).setCellValue("So ACE");
            header.createCell(3).setCellValue("Ngat ke thua");
            header.createCell(4).setCellValue("Ai co quyen gi");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("\\HR");
            row.createCell(1).setCellValue("Directory");
            row.createCell(2).setCellValue(1);
            row.createCell(3).setCellValue("Khong");
            row.createCell(4).setCellValue("FRA\\P.HR: Sua");
            workbook.write(output);
        }

        var rows = new ArrayList<RawImportRow>();
        new ExcelImportFileParser().parse(workbookPath, rows::add);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().rowNumber()).isEqualTo(2);
        assertThat(rows.getFirst().aceCount()).isEqualTo("1");
        assertThat(rows.getFirst().permissions()).isEqualTo("FRA\\P.HR: Sua");
    }
}
