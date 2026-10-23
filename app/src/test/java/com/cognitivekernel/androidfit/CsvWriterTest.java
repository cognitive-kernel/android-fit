package com.cognitivekernel.androidfit;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class CsvWriterTest {
    @Test public void quotesCommasQuotesAndLineBreaks() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter.writeRow(out, "plain", "name, with comma", "say \"hello\"", "two\nlines", null);
        assertEquals("\"plain\",\"name, with comma\",\"say \"\"hello\"\"\",\"two\nlines\",\"\"\n",
                new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test public void neutralizesSpreadsheetFormulaPrefixes() {
        assertEquals("'=1+1", CsvWriter.neutralizeFormula("=1+1"));
        assertEquals("'+SUM(A1:A2)", CsvWriter.neutralizeFormula("+SUM(A1:A2)"));
        assertEquals("' -10", CsvWriter.neutralizeFormula(" -10"));
        assertEquals("'@SUM(A1)", CsvWriter.neutralizeFormula("@SUM(A1)"));
    }

    @Test public void preservesOrdinaryTextAndNumbers() {
        assertEquals("Squat", CsvWriter.neutralizeFormula("Squat"));
        assertEquals("123.5", CsvWriter.neutralizeFormula("123.5"));
        assertEquals("", CsvWriter.neutralizeFormula(""));
    }

    @Test public void writesPersianTextAsUtf8() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter.writeRow(out, "اسکوات");
        assertEquals("\"اسکوات\"\n", new String(out.toByteArray(), StandardCharsets.UTF_8));
    }
}
