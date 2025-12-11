package com.elicitsoftware.response.pdf;

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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Represents a PDF document structure for report generation.
 * <p>
 * This class defines the complete structure of a PDF document including
 * its content, styling, layout settings, and metadata. It is designed
 * to be serialized to JSON for PDF generation services and includes
 * configuration for page orientation, breaks, and custom styling.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PDFDocument {

    /**
     * Default constructor.
     */
    public PDFDocument() {
        // Default constructor
    }

    /**
     * Array of content elements that make up the PDF document.
     * Each Content object represents a distinct element like text, tables, or images.
     */
    public Content[] content;

    /**
     * Indicates whether a page break should be applied.
     * Default is false.
     */
    public boolean pageBreak = false;

    /**
     * Indicates whether the document should be in landscape orientation.
     * Default is false (portrait orientation).
     */
    public boolean landscape = false;

    /**
     * The title of the PDF document.
     * This may be used for document metadata and headers.
     */
    public String title;

    /**
     * Map of named styles that can be referenced by content elements.
     * Keys are style names, values are Style objects defining formatting rules.
     */
    public Map<String, Style> styles;
}
