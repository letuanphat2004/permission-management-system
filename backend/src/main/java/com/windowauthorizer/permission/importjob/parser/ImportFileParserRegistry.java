package com.windowauthorizer.permission.importjob.parser;

import com.windowauthorizer.permission.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

@Component
public class ImportFileParserRegistry implements ImportFileParser {
    private final CsvImportFileParser csvParser;
    private final ExcelImportFileParser excelParser;

    public ImportFileParserRegistry(CsvImportFileParser csvParser, ExcelImportFileParser excelParser) {
        this.csvParser = csvParser;
        this.excelParser = excelParser;
    }

    @Override
    public void parse(Path path, Consumer<RawImportRow> rowConsumer) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) {
            csvParser.parse(path, rowConsumer);
        } else if (name.endsWith(".xls") || name.endsWith(".xlsx")) {
            excelParser.parse(path, rowConsumer);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE",
                    "Chỉ hỗ trợ file .csv, .xls hoặc .xlsx.");
        }
    }
}
