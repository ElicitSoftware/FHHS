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

/**
 * Represents a content element within a PDF document.
 * <p>
 * This class defines various types of content that can be included in a PDF,
 * such as text, SVG graphics, tables, and styled content. Each Content object
 * represents a single element that will be rendered in the final PDF document.
 * </p>
 * <p>
 * The class supports multiple content types but typically only one type
 * is used per instance (text, svg, or table).
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Content {

    /**
     * Text content to be rendered in the PDF.
     * Used for simple text elements, titles, paragraphs, etc.
     */
    public String text;

    /**
     * SVG content for vector graphics.
     * Used for charts, diagrams, or other scalable graphics.
     */
    public String svg;

    /**
     * Table content for structured data presentation.
     * Used for displaying tabular data with headers and rows.
     */
    public Table table;

    /**
     * Style reference for applying formatting.
     * References a named style from the PDFDocument's styles map.
     */
    public String style;

    /**
     * Default constructor for creating an empty Content object.
     */
    public Content() {
        super();
    }

    /**
     * Constructs a Content object containing a table.
     * <p>
     * This constructor is commonly used when the content element
     * is specifically meant to display tabular data.
     * </p>
     *
     * @param table the Table object to include in this content element
     */
    public Content(Table table) {
        super();
        this.table = table;
    }
}
