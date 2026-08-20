package com.windowauthorizer.permission.importjob.parser;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface ImportFileParser {
    void parse(Path path, Consumer<RawImportRow> rowConsumer);
}
