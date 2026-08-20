package com.windowauthorizer.permission.importjob.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class CsvImportFileParser {

    public void parse(Path path, Consumer<RawImportRow> rowConsumer) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            var iterator = parser.iterator();
            if (!iterator.hasNext()) {
                throw new ImportFileFormatException(1, "HEADER", "EMPTY_FILE", null, "File không có dữ liệu.");
            }

            CSVRecord header = iterator.next();
            HeaderValidator.validate(values(header));

            while (iterator.hasNext()) {
                CSVRecord record = iterator.next();
                if (record.size() < 5) {
                    throw new ImportFileFormatException(record.getRecordNumber(), "ROW", "MISSING_COLUMNS",
                            record.toString(), "Dòng dữ liệu không đủ 5 cột.");
                }
                rowConsumer.accept(new RawImportRow(
                        record.getRecordNumber(), record.get(0), record.get(1), record.get(2),
                        record.get(3), record.get(4)
                ));
            }
        } catch (ImportFileFormatException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw new ImportFileFormatException(0, "FILE", "INVALID_CSV", null,
                    "Không thể đọc cấu trúc file CSV.", exception);
        }
    }

    private List<String> values(CSVRecord record) {
        List<String> values = new ArrayList<>(record.size());
        record.forEach(values::add);
        return values;
    }
}
