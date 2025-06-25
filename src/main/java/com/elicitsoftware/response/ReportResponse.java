package com.elicitsoftware.response;

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

import com.elicitsoftware.response.pdf.PDFDocument;

/**
 * Response object containing report data in multiple formats.
 * <p>
 * This class encapsulates the complete response from a report generation
 * request, including the report title, HTML content for web display,
 * and a PDF document for printing or download. It provides a comprehensive
 * package of report data suitable for various consumption patterns.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class ReportResponse {

    /**
     * The title of the generated report.
     */
    public String title;

    /**
     * The HTML content of the report for web display.
     * Contains structured HTML with appropriate CSS classes for styling.
     */
    public String innerHTML;

    /**
     * The PDF document representation of the report.
     * Contains all necessary content, styles, and formatting for PDF generation.
     */
    public PDFDocument pdf;

    /**
     * Default constructor for creating an empty ReportResponse.
     */
    public ReportResponse(){
        super();
    }

    /**
     * Constructs a ReportResponse with the specified title, HTML content, and PDF document.
     *
     * @param title the report title
     * @param innerHTML the HTML content for web display
     * @param pdf the PDF document representation
     */
    public ReportResponse(String title, String innerHTML, PDFDocument pdf) {
        super();
        this.title = title;
        this.innerHTML = innerHTML;
        this.pdf = pdf;
    }
}
