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

/**
 * Extended PDPage class that adds cursor tracking functionality for PDF generation.
 * This class extends the standard PDFBox PDPage and adds a Y-coordinate cursor
 * to track the current vertical position when adding content to the page.
 */
public class PDFPage extends PDPage {
    /** The current Y-coordinate cursor position on the page for content placement. */
    public float cursorY = 0;

    /**
     * Default constructor that creates a new PDFPage.
     * Initializes the cursor position to 0.
     */
    public PDFPage() {
        super();
    }
}
