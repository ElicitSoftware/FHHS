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

public class ReportResponse {
    public String title;
    public String innerHTML;
    public PDFDocument pdf;

    public ReportResponse(){
        super();
    }

    public ReportResponse(String title, String innerHTML, PDFDocument pdf) {
        super();
        this.title = title;
        this.innerHTML = innerHTML;
        this.pdf = pdf;
    }
}
