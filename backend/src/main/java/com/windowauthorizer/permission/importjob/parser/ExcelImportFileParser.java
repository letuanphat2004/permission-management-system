package com.windowauthorizer.permission.importjob.parser;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class ExcelImportFileParser {

    public void parse(Path path, Consumer<RawImportRow> rowConsumer) {
        DataFormatter formatter = new DataFormatter();
        try (InputStream input = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ImportFileFormatException(1, "HEADER", "EMPTY_FILE", null, "File Excel không có sheet.");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new ImportFileFormatException(1, "HEADER", "EMPTY_FILE", null, "File Excel không có dữ liệu.");
            }
            HeaderValidator.validate(rowValues(header, formatter));

            for (int index = header.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || isEmpty(row, formatter)) {
                    continue;
                }
                rowConsumer.accept(new RawImportRow(index + 1,
                        cell(row, 0, formatter), cell(row, 1, formatter), cell(row, 2, formatter),
                        cell(row, 3, formatter), cell(row, 4, formatter)));
            }
        } catch (ImportFileFormatException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ImportFileFormatException(0, "FILE", "INVALID_EXCEL", null,
                    "Không thể đọc cấu trúc file Excel.", exception);
        }
    }

    private List<String> rowValues(Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>();
        for (int index = 0; index < Math.max(5, row.getLastCellNum()); index++) {
            values.add(cell(row, index, formatter));
        }
        return values;
    }

    private boolean isEmpty(Row row, DataFormatter formatter) {
        for (int index = 0; index < 5; index++) {
            if (!cell(row, index, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cell(Row row, int index, DataFormatter formatter) {
        return formatter.formatCellValue(row.getCell(index)).trim();
    }
}
