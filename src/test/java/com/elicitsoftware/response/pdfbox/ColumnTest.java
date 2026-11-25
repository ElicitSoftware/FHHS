package com.elicitsoftware.response.pdfbox;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnTest {

    @Test
    void constructor_setsNameAndWidth() {
        Column column = new Column("Person", 120f);

        assertEquals("Person", column.getName());
        assertEquals(120f, column.getWidth());
    }

    @Test
    void setters_updateNameAndWidth() {
        Column column = new Column("Person", 120f);

        column.setName("Relationship");
        column.setWidth(200f);

        assertEquals("Relationship", column.getName());
        assertEquals(200f, column.getWidth());
    }
}
