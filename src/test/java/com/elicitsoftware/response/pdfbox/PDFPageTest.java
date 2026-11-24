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

import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PDFPageTest {

    @Test
    void defaultConstructor_startsWithCursorYAtZero() {
        PDFPage page = new PDFPage();

        assertEquals(0f, page.cursorY);
    }

    @Test
    void isPDPage_forInteropWithPdfbox() {
        PDFPage page = new PDFPage();

        assertTrue(page instanceof PDPage);
    }

    @Test
    void cursorY_isMutable() {
        PDFPage page = new PDFPage();

        page.cursorY = 42f;

        assertEquals(42f, page.cursorY);
    }
}
