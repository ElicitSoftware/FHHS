package com.elicitsoftware.familyhistory;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.response.pdf.Content;
import com.elicitsoftware.response.pdfbox.Table;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the two pieces of {@link PDFService} that don't require a live database,
 * network REST client, or Respondent/ReportDefinition entities:
 * {@code wrapText} (public static) and {@code createContent} (private static, via reflection).
 */
class PDFServiceTest {

    private final PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final float FONT_SIZE = 10f;

    // ------------------------------------------------------------------
    // wrapText
    // ------------------------------------------------------------------

    @Test
    void wrapText_nullText_returnsEmptyList() throws IOException {
        assertTrue(PDFService.wrapText(null, font, FONT_SIZE, 200f).isEmpty());
    }

    @Test
    void wrapText_emptyText_returnsEmptyList() throws IOException {
        assertTrue(PDFService.wrapText("", font, FONT_SIZE, 200f).isEmpty());
    }

    @Test
    void wrapText_shortTextThatFits_returnsSingleUnchangedLine() throws IOException {
        List<String> lines = PDFService.wrapText("Hello world", font, FONT_SIZE, 200f);

        assertEquals(List.of("Hello world"), lines);
    }

    @Test
    void wrapText_longText_wrapsIntoMultipleLinesEachWithinMaxWidth() throws IOException {
        String text = "The quick brown fox jumps over the lazy dog repeatedly for many more words";
        float maxWidth = 80f;

        List<String> lines = PDFService.wrapText(text, font, FONT_SIZE, maxWidth);

        assertTrue(lines.size() > 1, "expected wrapping into multiple lines, got: " + lines);
        for (String line : lines) {
            float width = font.getStringWidth(line) / 1000 * FONT_SIZE;
            assertTrue(width <= maxWidth, "line '" + line + "' (" + width + ") exceeds maxWidth " + maxWidth);
        }
        // Rejoining the wrapped lines must reproduce the original words in order, none dropped.
        assertEquals(text, String.join(" ", lines));
    }

    @Test
    void wrapText_singleWordLongerThanMaxWidth_isNotDroppedButLeadsWithAnEmptyLine() throws IOException {
        // Reading the algorithm: on the very first word, currentLine is still empty when the
        // over-width check fires, so it unconditionally does lines.add(currentLine.toString())
        // BEFORE ever appending the word - adding an empty string - then seeds currentLine with
        // the oversized word, which is flushed as its own (still oversized) line at the end.
        // This is a real quirk of the current implementation, verified by running this test,
        // not assumed from reading alone.
        String longWord = "Supercalifragilisticexpialidocious";
        float maxWidth = 10f; // smaller than the word's rendered width at FONT_SIZE

        List<String> lines = PDFService.wrapText(longWord, font, FONT_SIZE, maxWidth);

        assertEquals(List.of("", longWord), lines, "single overlong word: leading empty line + the word itself, unwrapped");
    }

    @Test
    void wrapText_multipleSpaces_produceEmptyWordsThatAreHarmlesslyIncludedAsZeroWidthTokens() throws IOException {
        // text.split(" ") on "a  b" (double space) yields ["a", "", "b"] - the empty middle
        // "word" has zero width so it never triggers a wrap by itself; confirms no crash.
        List<String> lines = PDFService.wrapText("a  b", font, FONT_SIZE, 200f);

        assertEquals(1, lines.size());
        assertEquals("a  b", lines.get(0));
    }

    // ------------------------------------------------------------------
    // createContent (private static) via reflection
    // ------------------------------------------------------------------

    private Table invokeCreateContent(Content content) throws Exception {
        Method method = PDFService.class.getDeclaredMethod("createContent", Content.class);
        method.setAccessible(true);
        try {
            return (Table) method.invoke(null, content);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }

    private com.elicitsoftware.response.pdf.Table buildPdfTable(String[] headers, float[] widths, String[][] body) {
        com.elicitsoftware.response.pdf.Table table = new com.elicitsoftware.response.pdf.Table();
        table.headers = headers;
        table.widths = widths;
        table.body = body;
        return table;
    }

    @Test
    void createContent_columnCountMatchesHeaderCount() throws Exception {
        Content content = new Content(buildPdfTable(
                new String[]{"Person", "Cancer", "Age"},
                new float[]{0f, 300f, 100f},
                new String[][]{{"Mother", "Breast Cancer", "45"}}
        ));

        Table table = invokeCreateContent(content);

        assertEquals(3, table.getColumns().size());
    }

    @Test
    void createContent_firstColumnAutoSizesFromFontMetricsNotFromProvidedWidth() throws Exception {
        // widths[0] is deliberately set to an absurd sentinel (9999f) that must be ignored -
        // column 0's width instead comes from measuring the header/body text.
        Content content = new Content(buildPdfTable(
                new String[]{"Person", "Cancer"},
                new float[]{9999f, 300f},
                new String[][]{{"Mother", "Breast Cancer"}}
        ));

        Table table = invokeCreateContent(content);

        float col0Width = table.getColumns().get(0).getWidth();
        assertTrue(col0Width > 0f && col0Width < 9999f,
                "column 0 width should be derived from font metrics, not the sentinel widths[0]: " + col0Width);
    }

    @Test
    void createContent_nonFirstColumns_useExactlyTheProvidedWidths() throws Exception {
        Content content = new Content(buildPdfTable(
                new String[]{"Person", "Cancer", "Age"},
                new float[]{0f, 300f, 75f},
                new String[][]{{"Mother", "Breast Cancer", "45"}}
        ));

        Table table = invokeCreateContent(content);

        assertEquals(300f, table.getColumns().get(1).getWidth());
        assertEquals(75f, table.getColumns().get(2).getWidth());
    }

    @Test
    void createContent_columnHeaderNamesPreserved() throws Exception {
        Content content = new Content(buildPdfTable(
                new String[]{"Person", "Cancer"},
                new float[]{0f, 300f},
                new String[][]{{"Mother", "Breast Cancer"}}
        ));

        Table table = invokeCreateContent(content);

        assertEquals("Person", table.getColumns().get(0).getName());
        assertEquals("Cancer", table.getColumns().get(1).getName());
    }

    @Test
    void createContent_widerBodyTextThanHeader_growsFirstColumnWidth() throws Exception {
        Content shortBody = new Content(buildPdfTable(
                new String[]{"Person", "Cancer"},
                new float[]{0f, 300f},
                new String[][]{{"Mo", "Breast Cancer"}}
        ));
        Content longBody = new Content(buildPdfTable(
                new String[]{"Person", "Cancer"},
                new float[]{0f, 300f},
                new String[][]{{"Maternal Grandmother", "Breast Cancer"}}
        ));

        float shortWidth = invokeCreateContent(shortBody).getColumns().get(0).getWidth();
        float longWidth = invokeCreateContent(longBody).getColumns().get(0).getWidth();

        assertTrue(longWidth > shortWidth,
                "a longer first-column value must produce a wider auto-sized column: short=" + shortWidth + " long=" + longWidth);
    }
}
