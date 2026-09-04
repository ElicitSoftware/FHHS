package com.elicitsoftware.response.pdfbox;

/*-
 * ***LICENSE_START***
 * Elicit Survey
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link PDFTableGenerator} against real in-memory Apache PDFBox {@link PDDocument}s -
 * no mocking, no external I/O needed since PDFBox itself has no network/DB dependency.
 */
class PDFTableGeneratorTest {

    private final PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private PDDocument document;

    @AfterEach
    void closeDocument() throws IOException {
        if (document != null) {
            document.close();
        }
    }

    private Table buildTable(float height, float rowHeight, int numberOfRows, String[][] content, boolean landscape) {
        List<Column> columns = List.of(new Column("Person", 100f), new Column("Cancer", 300f));
        return new TableBuilder()
                .setHeight(height)
                .setRowHeight(rowHeight)
                .setNumberOfRows(numberOfRows)
                .setContent(content)
                .setColumns(columns)
                .setCellMargin(2f)
                .setMargin(40f)
                .setPageSize(PDRectangle.LETTER)
                .setLandscape(landscape)
                .setTextFont(font)
                .setFontSize(10f)
                .build();
    }

    @Test
    void generatePDF_smallTableThatFitsOnePage_doesNotThrowAndProducesSavableDocument() throws IOException {
        document = new PDDocument();
        document.addPage(new PDPage(PDRectangle.LETTER));
        String[][] content = {{"Mother", "Breast Cancer"}};
        Table table = buildTable(700f, 15f, content.length, content, false);

        new PDFTableGenerator().generatePDF(document, table);

        assertDoesNotThrow(() -> document.save(new ByteArrayOutputStream()));
    }

    @Test
    void drawTable_contentExceedingOnePageWorthOfRows_stillCompletesWithoutThrowing() throws IOException {
        // rowsPerPage = floor(height / rowHeight) - 1 = floor(30/10) - 1 = 2
        // numberOfPages = ceil(5 / 2) = 3 iterations of drawCurrentPage()
        document = new PDDocument();
        document.addPage(new PDPage(PDRectangle.LETTER));
        String[][] content = {
                {"Mother", "Breast Cancer"},
                {"Father", "Lung Cancer"},
                {"Sibling", "Melanoma"},
                {"Aunt", "Ovarian Cancer"},
                {"Uncle", "Prostate Cancer"}
        };
        Table table = buildTable(30f, 10f, content.length, content, false);

        new PDFTableGenerator().generatePDF(document, table);

        // NOTE: PDFTableGenerator.drawTable() always draws onto document.getPage(getNumberOfPages()-1)
        // using PDPageContentStream.AppendMode.OVERWRITE and never calls document.addPage() itself -
        // unlike the near-duplicate drawTable() in PDFService. So even though the pagination math
        // computes 3 iterations, this generator does NOT grow the document to 3 pages; each
        // iteration overwrites the same single page. This is verified behavior, not an assumption.
        assertEquals(1, document.getNumberOfPages(),
                "PDFTableGenerator never calls document.addPage() - confirmed by running this test");
        assertDoesNotThrow(() -> document.save(new ByteArrayOutputStream()));
    }

    @Test
    void generatePDF_landscapeTable_doesNotThrow() throws IOException {
        document = new PDDocument();
        document.addPage(new PDPage(PDRectangle.LETTER));
        String[][] content = {{"Mother", "Breast Cancer"}};
        Table table = buildTable(700f, 15f, content.length, content, true);

        assertDoesNotThrow(() -> new PDFTableGenerator().generatePDF(document, table));
        assertDoesNotThrow(() -> document.save(new ByteArrayOutputStream()));
    }

    @Test
    void generatePDF_portraitTable_doesNotThrow() throws IOException {
        document = new PDDocument();
        document.addPage(new PDPage(PDRectangle.LETTER));
        String[][] content = {{"Mother", "Breast Cancer"}};
        Table table = buildTable(700f, 15f, content.length, content, false);

        assertDoesNotThrow(() -> new PDFTableGenerator().generatePDF(document, table));
    }

    @Test
    void generatePDF_emptyContent_producesZeroPagesOfDrawingWithoutThrowing() throws IOException {
        document = new PDDocument();
        document.addPage(new PDPage(PDRectangle.LETTER));
        String[][] content = {};
        Table table = buildTable(700f, 15f, 0, content, false);

        assertDoesNotThrow(() -> new PDFTableGenerator().generatePDF(document, table));
        assertEquals(1, document.getNumberOfPages());
    }
}
