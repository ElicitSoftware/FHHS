package com.elicitsoftware.proband;

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

import com.elicitsoftware.model.Card;
import com.elicitsoftware.model.FactFHHSView;
import com.elicitsoftware.model.Row;
import com.elicitsoftware.request.ReportRequest;
import com.elicitsoftware.response.ReportResponse;
import com.elicitsoftware.response.pdf.Content;
import com.elicitsoftware.response.pdf.PDFDocument;
import com.elicitsoftware.response.pdf.Style;
import com.elicitsoftware.response.pdf.Table;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST service for generating proband (respondent) summary reports.
 * <p>
 * This service provides endpoints for creating comprehensive reports about
 * study participants (probands). It generates both HTML and PDF formats
 * of the respondent summary, including family history health survey data.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@Path("/proband")
@RequestScoped
public class Service {

    /**
     * String buffer for building PDF table content.
     */
    private StringBuffer pdfTableSB = null;

    /**
     * The default title for respondent summary reports.
     */
    private static String TITLE = "Respondent Summary";

    /**
     * Default constructor.
     */
    public Service() {
        super();
    }

    /**
     * Generates a comprehensive report for a specific proband/respondent.
     * <p>
     * This endpoint retrieves family history health survey data for the specified
     * respondent and generates both HTML and PDF format reports. The report includes
     * all relevant proband information in a structured table format.
     * </p>
     *
     * @param req the report request containing the respondent ID
     * @return a ReportResponse containing the title, HTML content, and PDF document
     * @throws jakarta.ws.rs.WebApplicationException if the user lacks proper authorization
     */
    @Path("/report")
    @POST
    @RolesAllowed("proband-user")
    @Consumes("application/json")
    @Produces("application/json")
    @Transactional
    public ReportResponse report(ReportRequest req) {

        Table table = new Table();
        table.headers = new String[2];
        table.headers[0] = "Property";
        table.headers[1] = "Value";

        table.widths = new float[2];
        table.widths[0] = 300;
        table.widths[1] = 200f;

        List<Row> pdfRows = new ArrayList<Row>();

        pdfTableSB = new StringBuffer();
        String title = "";

        StringBuffer innerHTML = new StringBuffer();

        FactFHHSView fact = FactFHHSView.find("respondent_id =?1 and step = 'Proband'", req.id).firstResult();

        if (fact != null) {
            List<Row> rows = fact.getProbandRows();
            table.body = new String[rows.size()][2];
            Card card = new Card(title);
            card.addRows(rows);
            int i = 0;

            for (Row row : rows) {
                setPDFTableRow(row, table, i);
                i++;
            }

            innerHTML.append(card.getHTML());

        } else {
            innerHTML.append("No Significant Data.");
            pdfTableSB.append("[{ text: 'No significant data to report.', bold: true }, '', '']");
        }

        PDFDocument pdf = new PDFDocument();
        pdf.title = TITLE;
        pdf.content = getPDFContent(table);
        pdf.styles = getPDFStyles();

        return new ReportResponse(TITLE, innerHTML.toString(), pdf);
    }

    /**
     * Sets a single row of data in the PDF table structure.
     * <p>
     * This helper method populates the table body array with label and value
     * data from a Row object at the specified index position.
     * </p>
     *
     * @param row the Row object containing label and value data
     * @param table the Table object to populate
     * @param index the zero-based index position for this row in the table
     */
    private void setPDFTableRow(Row row, Table table, int index) {
        table.body[index][0] = row.getLabel();
        table.body[index][1] = row.getValue();
    }

    /**
     * Creates a map of style configurations used for formatting PDF document elements.
     * Each entry in the map associates a style name with a corresponding Style object.
     * <p>
     * The implemented styles include:
     * 1. "ca_title": A style with larger font size, bold formatting, and specific margins.
     * 2. "ca_bold": A style with a smaller font size and specific margins.
     *
     * @return A map containing style configurations referenced by their corresponding style names.
     */
    private Map<String, Style> getPDFStyles() {

        HashMap<String, Style> styles = new HashMap<String, Style>();

        Style title = new Style();
        title.fontSize = 16;
        title.bold = true;
        title.margin = new Integer[]{0, 40, 0, 10};
        styles.put("ca_title", title);

        Style report = new Style();
        report.fontSize = 12;
        report.margin = new Integer[]{0, 0, 0, 40};
        styles.put("ca_bold", report);
        return styles;
    }

    /**
     * Generates an array of `Content` objects for inclusion in a PDF document,
     * specifically representing a title and a table. The first element is a
     * title with predefined text and style, and the second element is a table.
     *
     * @param table the `Table` object containing header rows, column widths,
     *              and body data to be converted into a `Content` object.
     * @return an array of `Content` objects where the first element is a title
     * and the second element is the converted `Table` object.
     */
    public Content[] getPDFContent(Table table) {
        Content[] content = new Content[1];
        content[0] = new Content(table);
        return content;
    }
}
