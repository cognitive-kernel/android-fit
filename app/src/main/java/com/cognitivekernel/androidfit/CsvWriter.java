package com.cognitivekernel.androidfit;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Writes RFC-4180-style UTF-8 CSV rows and neutralizes spreadsheet formula cells. */
public final class CsvWriter {
    private CsvWriter() { }

    public static void writeRow(OutputStream out, String... cells) throws IOException {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) out.write(',');
            String value = cells[i] == null ? "" : cells[i];
            value = neutralizeFormula(value);
            out.write(("\"" + value.replace("\"", "\"\"") + "\"").getBytes(StandardCharsets.UTF_8));
        }
        out.write('\n');
    }

    static String neutralizeFormula(String value) {
        int i = 0;
        while (i < value.length() && Character.isWhitespace(value.charAt(i))) i++;
        if (i < value.length()) {
            char first = value.charAt(i);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                return "'" + value;
            }
        }
        return value;
    }
}
