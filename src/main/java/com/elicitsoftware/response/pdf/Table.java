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

/**
 * Represents a table structure for PDF document generation.
 * <p>
 * This class defines the basic structure of a table including headers,
 * column widths, and body content. It's used as part of the PDF content
 * generation process to create structured tabular data in reports.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class Table {

    /**
     * Default constructor.
     */
    public Table() {
        // Default constructor
    }

    /**
     * Array of header labels for the table columns.
     */
    public String[] headers;

    /**
     * Array of column widths for controlling table layout.
     * Values typically represent width in points or relative proportions.
     */
    public float[] widths;

    /**
     * Two-dimensional array containing the table body content.
     * First dimension represents rows, second dimension represents columns.
     */
    public String[][] body;
}
