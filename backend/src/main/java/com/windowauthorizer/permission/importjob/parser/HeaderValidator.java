package com.windowauthorizer.permission.importjob.parser;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

final class HeaderValidator {
    private static final List<String> EXPECTED = List.of(
            "DUONG DAN", "LOAI", "SO ACE", "NGAT KE THUA", "AI CO QUYEN GI"
    );

    private HeaderValidator() {
    }

    static void validate(List<String> headers) {
        if (headers.size() < EXPECTED.size()) {
            throw invalid(headers);
        }
        for (int index = 0; index < EXPECTED.size(); index++) {
            if (!normalize(headers.get(index)).equals(EXPECTED.get(index))) {
                throw invalid(headers);
            }
        }
    }

    private static ImportFileFormatException invalid(List<String> headers) {
        return new ImportFileFormatException(1, "HEADER", "INVALID_HEADER", String.join(", ", headers),
                "File phải có 5 cột đầu: Duong dan, Loai, So ACE, Ngat ke thua, Ai co quyen gi.");
    }

    private static String normalize(String value) {
        String text = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace("Đ", "D").replace("đ", "d")
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return text.toUpperCase(Locale.ROOT);
    }
}
