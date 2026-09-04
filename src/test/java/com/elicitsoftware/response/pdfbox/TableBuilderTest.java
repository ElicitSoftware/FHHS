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

import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableBuilderTest {

    @Test
    void build_populatesEveryFieldFromTheFluentChain() {
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        List<Column> columns = List.of(new Column("Person", 100f), new Column("Cancer", 300f));
        String[][] content = {{"Mother", "Breast Cancer"}};

        Table table = new TableBuilder()
                .setHeight(700f)
                .setNumberOfRows(1)
                .setRowHeight(15f)
                .setContent(content)
                .setColumns(columns)
                .setCellMargin(2f)
                .setMargin(40f)
                .setPageSize(PDRectangle.LETTER)
                .setLandscape(true)
                .setTextFont(font)
                .setFontSize(10f)
                .build();

        assertEquals(700f, table.getHeight());
        assertEquals(1, table.getNumberOfRows());
        assertEquals(15f, table.getRowHeight());
        assertSame(content, table.getContent());
        assertSame(columns, table.getColumns());
        assertEquals(2f, table.getCellMargin());
        assertEquals(40f, table.getMargin());
        assertSame(PDRectangle.LETTER, table.getPageSize());
        assertTrue(table.isLandscape());
        assertSame(font, table.getTextFont());
        assertEquals(10f, table.getFontSize());
    }

    @Test
    void build_defaultLandscapeIsFalse() {
        Table table = new TableBuilder().build();

        assertFalse(table.isLandscape());
    }

    @Test
    void build_returnsANewTableEachTime() {
        TableBuilder builderA = new TableBuilder();
        TableBuilder builderB = new TableBuilder();

        assertNotSame(builderA.build(), builderB.build());
    }
}
