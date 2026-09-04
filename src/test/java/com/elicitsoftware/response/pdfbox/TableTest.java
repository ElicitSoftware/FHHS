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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TableTest {

    @Test
    void getNumberOfColumns_matchesColumnsListSize() {
        Table table = new Table();
        table.setColumns(List.of(new Column("A", 10f), new Column("B", 20f), new Column("C", 30f)));

        assertEquals(3, table.getNumberOfColumns());
    }

    @Test
    void getWidth_sumsAllColumnWidths() {
        Table table = new Table();
        table.setColumns(List.of(new Column("A", 100f), new Column("B", 200f), new Column("C", 50f)));

        assertEquals(350f, table.getWidth());
    }

    @Test
    void getWidth_singleColumn_returnsThatColumnsWidth() {
        Table table = new Table();
        table.setColumns(List.of(new Column("A", 42f)));

        assertEquals(42f, table.getWidth());
    }

    @Test
    void getColumnsNamesAsArray_preservesColumnOrder() {
        Table table = new Table();
        table.setColumns(List.of(new Column("Person", 100f), new Column("Cancer", 300f), new Column("Age", 100f)));

        assertArrayEquals(new String[]{"Person", "Cancer", "Age"}, table.getColumnsNamesAsArray());
    }
}
