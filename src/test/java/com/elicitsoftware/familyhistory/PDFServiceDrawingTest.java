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
import com.elicitsoftware.response.pdfbox.Column;
import com.elicitsoftware.response.pdfbox.Table;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PDFService}'s PDFBox drawing methods ({@code addTitleBlock},
 * {@code addTextBlock}, {@code addSVG}, {@code addHeadersAndFooters}, {@code drawTable}), driven
 * directly against a hand-primed document/page/contentStream (the same setup
 * {@code generatePDF(long)} performs before its report loop) rather than through
 * {@code generatePDF} itself, which needs a live {@code Respondent} lookup and a real report
 * REST client - see {@code PDFServiceCallReportTest} for {@code callReport}, the piece of
 * {@code generatePDF} that calls out to each configured report per BR-004.
 */
class PDFServiceDrawingTest {

    private PDFService newPrimedService() throws IOException {
        PDFService service = new PDFService();
        service.baseUrl = "http://localhost:8080/";
        service.document = new PDDocument();
        service.page = new PDPage(PDRectangle.LETTER);
        service.document.addPage(service.page);
        service.contentStream = new PDPageContentStream(service.document, service.page);
        service.contentStream.setFont(PDFService.TEXT_FONT, PDFService.FONT_SIZE);
        service.pageHeight = PDRectangle.LETTER.getHeight();
        service.pageWidth = PDRectangle.LETTER.getWidth();
        service.yPosition = service.pageHeight - PDFService.TEXT_MARGIN;
        return service;
    }

    private String finishAndExtractText(PDFService service) throws IOException {
        try {
            service.contentStream.close();
        } catch (Exception alreadyClosed) {
            // addSVG closes its own content stream in a finally block before returning
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.document.save(out);
        service.document.close();
        try (PDDocument reloaded = Loader.loadPDF(out.toByteArray())) {
            return new PDFTextStripper().getText(reloaded);
        }
    }

    /**
     * Like {@link #finishAndExtractText}, but keeps each physical page's text separate -
     * needed to prove which page a row actually landed on, which a whole-document string can't.
     */
    private List<String> finishAndExtractTextPerPage(PDFService service) throws IOException {
        try {
            service.contentStream.close();
        } catch (Exception alreadyClosed) {
            // addSVG closes its own content stream in a finally block before returning
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.document.save(out);
        int pageCount = service.document.getNumberOfPages();
        service.document.close();

        List<String> perPageText = new ArrayList<>();
        try (PDDocument reloaded = Loader.loadPDF(out.toByteArray())) {
            for (int page = 1; page <= pageCount; page++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                perPageText.add(stripper.getText(reloaded));
            }
        }
        return perPageText;
    }

    /**
     * Builds a {@code pdfbox.Table} the same way {@code PDFService.createContent} does: each
     * row's height is computed by wrapping its cells against their column's width, so a cell
     * wider than its column grows that row instead of overflowing into the next column.
     */
    private Table buildPdfboxTable(String[] headers, float[] columnWidths, String[][] body,
                                    float rowHeight, float tableHeight) throws IOException {
        List<Column> columns = new ArrayList<>();
        for (int i = 0; i < headers.length; i++) {
            columns.add(new Column(headers[i], columnWidths[i]));
        }
        float cellMargin = 2f;
        float[] rowHeights = new float[body.length];
        for (int r = 0; r < body.length; r++) {
            int maxLines = 1;
            for (int c = 0; c < body[r].length; c++) {
                List<String> lines = PDFService.wrapText(body[r][c], PDFService.TEXT_FONT,
                        PDFService.FONT_SIZE, columnWidths[c] - (2 * cellMargin));
                maxLines = Math.max(maxLines, Math.max(1, lines.size()));
            }
            rowHeights[r] = maxLines * rowHeight;
        }

        Table table = new Table();
        table.setColumns(columns);
        table.setContent(body);
        table.setRowHeights(rowHeights);
        table.setNumberOfRows(body.length);
        table.setRowHeight(rowHeight);
        table.setHeight(tableHeight);
        table.setMargin(PDFService.TEXT_MARGIN);
        table.setCellMargin(cellMargin);
        table.setPageSize(PDRectangle.LETTER);
        table.setLandscape(false);
        table.setTextFont(PDFService.TEXT_FONT);
        table.setFontSize(PDFService.FONT_SIZE);
        return table;
    }

    // ------------------------------------------------------------------
    // addTitleBlock
    // ------------------------------------------------------------------

    @Test
    void addTitleBlock_nonEmptyTitle_writesTextAndAdvancesYPosition() throws IOException {
        PDFService service = newPrimedService();
        float yBefore = service.yPosition;

        service.addTitleBlock("Family Health History Summary");

        assertTrue(service.yPosition < yBefore, "yPosition should move down after writing a title");
        assertEquals(1, service.document.getNumberOfPages());
        assertTrue(finishAndExtractText(service).contains("Family Health History Summary"));
    }

    @Test
    void addTitleBlock_nullOrEmptyTitle_isNoOp() throws IOException {
        PDFService service = newPrimedService();
        float yBefore = service.yPosition;

        service.addTitleBlock(null);
        service.addTitleBlock("");

        assertEquals(yBefore, service.yPosition);
        assertEquals(1, service.document.getNumberOfPages());
    }

    @Test
    void addTitleBlock_whenTooCloseToPageBottom_startsNewPage() throws IOException {
        PDFService service = newPrimedService();
        service.yPosition = 55f; // minus HEADER_MARGIN(20) = 35, below the FONT_SIZE(10)+PADDING(40)=50 threshold

        service.addTitleBlock("Overflow Title");

        assertEquals(2, service.document.getNumberOfPages());
    }

    // ------------------------------------------------------------------
    // addTextBlock
    // ------------------------------------------------------------------

    @Test
    void addTextBlock_longText_wrapsAndAdvancesYPositionPerLine() throws IOException {
        PDFService service = newPrimedService();
        float yBefore = service.yPosition;
        String longText = "This paragraph is deliberately long enough that it must wrap across "
                + "several lines once rendered at the page's text width, advancing the cursor once per line.";

        service.addTextBlock(longText);

        // more than one line means the drop is bigger than a single FONT_SIZE+2 step plus the trailing LEADING
        assertTrue(yBefore - service.yPosition > PDFService.FONT_SIZE + 2 + PDFService.LEADING);
        String text = finishAndExtractText(service);
        assertTrue(text.contains("wrap across"));
    }

    @Test
    void addTextBlock_whenTooCloseToPageBottom_startsNewPageBeforeWriting() throws IOException {
        PDFService service = newPrimedService();
        service.yPosition = 45f; // below PADDING(40)+FONT_SIZE(10)=50 threshold

        service.addTextBlock("This text should land on a fresh page.");

        assertEquals(2, service.document.getNumberOfPages());
    }

    // ------------------------------------------------------------------
    // addSVG
    // ------------------------------------------------------------------

    @Test
    void addSVG_validSvg_convertsCurrentPageToLandscapeWithoutThrowing() throws IOException {
        PDFService service = newPrimedService();
        Content content = new Content();
        content.svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"100\">"
                + "<rect x=\"0\" y=\"0\" width=\"200\" height=\"100\" fill=\"blue\"/></svg>";

        assertDoesNotThrow(() -> service.addSVG(content));

        PDPage lastPage = service.document.getPage(service.document.getNumberOfPages() - 1);
        assertTrue(lastPage.getMediaBox().getWidth() > lastPage.getMediaBox().getHeight(),
                "addSVG should flip the current page to landscape");
        assertEquals(1, service.document.getNumberOfPages(), "addSVG re-uses the current page rather than adding one");
    }

    // ------------------------------------------------------------------
    // addHeadersAndFooters
    // ------------------------------------------------------------------

    @Test
    void addHeadersAndFooters_stampsPageNumberDateAndBaseUrlOnEveryPage() throws IOException {
        PDFService service = newPrimedService();
        service.addTitleBlock("Body content");

        service.addHeadersAndFooters();

        String text = finishAndExtractText(service);
        assertTrue(text.contains("Family Health History Survey - Page 1 of 1"));
        assertTrue(text.contains("Page 1 of 1"));
        assertTrue(text.contains("localhost:8080"), "footer should include the configured base URL: " + text);
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        assertTrue(text.contains(today), "footer should include today's date: " + text);
    }

    // ------------------------------------------------------------------
    // drawTable
    // ------------------------------------------------------------------

    @Test
    void drawTable_fitsOnOnePage_writesHeaderAndAllRowsWithoutAddingPages() throws IOException {
        PDFService service = newPrimedService();
        Table table = buildPdfboxTable(
                new String[]{"Person", "Diagnosis"},
                new float[]{100f, 100f},
                new String[][]{{"Mother", "Breast Cancer"}, {"Sister", "None"}},
                15f, 700f);

        service.drawTable(table);

        assertEquals(1, service.document.getNumberOfPages());
        String text = finishAndExtractText(service);
        assertTrue(text.contains("Person") && text.contains("Diagnosis"));
        assertTrue(text.contains("Mother") && text.contains("Breast Cancer"));
        assertTrue(text.contains("Sister"));
    }

    @Test
    void drawTable_moreRowsThanFitOnOnePage_paginatesRowsAcrossMultiplePhysicalPages() throws IOException {
        // Each row is 10pt tall, table height 100pt reserves 10pt for the header, leaving 90pt
        // of usable body height - 9 rows fit per page (9*10=90), so 25 rows need 3 pages:
        // rows 0-8, 9-17, 18-24.
        PDFService service = newPrimedService();
        String[][] body = new String[25][];
        for (int i = 0; i < body.length; i++) {
            body[i] = new String[]{"Relative " + i, "Condition " + i};
        }
        Table table = buildPdfboxTable(new String[]{"Person", "Diagnosis"}, new float[]{100f, 100f}, body, 10f, 100f);

        service.drawTable(table);

        assertEquals(3, service.document.getNumberOfPages());
        List<String> perPage = finishAndExtractTextPerPage(service);
        for (String pageText : perPage) {
            assertTrue(pageText.contains("Person") && pageText.contains("Diagnosis"),
                    "header should repeat on every page: " + pageText);
        }
        assertTrue(perPage.get(0).contains("Relative 0"));
        assertTrue(perPage.get(0).contains("Relative 8"));
        assertFalse(perPage.get(0).contains("Relative 9"));
        assertTrue(perPage.get(1).contains("Relative 9"));
        assertTrue(perPage.get(1).contains("Relative 17"));
        assertFalse(perPage.get(1).contains("Relative 18"));
        assertTrue(perPage.get(2).contains("Relative 18"));
        assertTrue(perPage.get(2).contains("Relative 24"));
    }

    @Test
    void drawTable_cellTextWraps_growsRowHeightAndPreservesAllLines() throws IOException {
        PDFService service = newPrimedService();
        String longDiagnosis = "Multiple Non-Hodgkins Lymphoma With Bone Marrow Involvement";
        Table table = buildPdfboxTable(
                new String[]{"Person", "Diagnosis"},
                new float[]{100f, 100f},
                new String[][]{{"Mother", longDiagnosis}, {"Sister", "Breast Cancer"}},
                15f, 700f);

        assertTrue(table.getRowHeights()[0] > table.getRowHeight(),
                "a cell wider than its column should grow that row's height");
        assertEquals(table.getRowHeight(), table.getRowHeights()[1],
                "a short cell's row should stay at the single-line height");

        service.drawTable(table);

        assertEquals(1, service.document.getNumberOfPages(), "the wrapped row alone shouldn't force a new page here");
        String text = finishAndExtractText(service);
        for (String word : longDiagnosis.split(" ")) {
            assertTrue(text.contains(word), "wrapped word '" + word + "' missing from: " + text);
        }
        assertTrue(text.contains("Sister") && text.contains("Breast Cancer"),
                "the row after the wrapped one must remain fully intact: " + text);
    }

    @Test
    void drawTable_mixedWrappedAndSingleLineRows_acrossMultiplePages() throws IOException {
        // Row heights: R0=10, R1=10, R2=20 (wraps to 2 lines), R3=10, R4=10.
        // tableHeight=45 reserves 10 for the header, leaving 35 usable: R0+R1=20 fits, +R2(20)
        // would make 40>35 so page 1 stops at [R0,R1]. Page 2: R2(20)+R3(10)=30<=35 fits, +R4
        // would make 40>35 so page 2 stops at [R2,R3]. Page 3: [R4] alone.
        PDFService service = newPrimedService();
        String wrappingText = "Diagnosis label wraps onto two lines";
        String[][] body = {
                {"R0", "short"},
                {"R1", "short"},
                {"R2", wrappingText},
                {"R3", "short"},
                {"R4", "short"},
        };
        Table table = buildPdfboxTable(new String[]{"Person", "Diagnosis"}, new float[]{100f, 100f}, body, 10f, 45f);
        assertEquals(20f, table.getRowHeights()[2], "sanity check: the long cell should wrap into exactly 2 lines");

        service.drawTable(table);

        assertEquals(3, service.document.getNumberOfPages());
        List<String> perPage = finishAndExtractTextPerPage(service);
        assertTrue(perPage.get(0).contains("R0") && perPage.get(0).contains("R1"));
        assertFalse(perPage.get(0).contains("R2"));
        assertTrue(perPage.get(1).contains("R2") && perPage.get(1).contains("R3"));
        assertFalse(perPage.get(1).contains("R4"));
        assertTrue(perPage.get(2).contains("R4"));
        // The wrapped row's text must still be fully present (not split across pages, not lost).
        for (String word : wrappingText.split(" ")) {
            assertTrue(perPage.get(1).contains(word), "wrapped word '" + word + "' missing from page 2");
        }
    }
}
