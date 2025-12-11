package com.elicitsoftware.casummary;

/*-
 * ***LICENSE_START***
 * Elicit FHHS     * </ul>
     *
     * @param req the report request containing the respondent ID%%
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
import io.quarkus.panache.common.Sort;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;

import java.util.*;
import java.util.Map.Entry;

/**
 * REST service for generating comprehensive cancer summary reports.
 * <p>
 * This service creates detailed cancer summary reports that aggregate and present
 * cancer history information across all family members. It organizes cancer data
 * by type and family member, showing ages at diagnosis and multiple cancer
 * occurrences in a structured, easy-to-read format.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@Path("casummary")
@RequestScoped
public class Service {

    /**
     * Default value used when age information is unknown.
     */
    private static final String UKN_AGE = "Unknown";

    /**
     * String buffer for building PDF table content.
     */
    private StringBuffer pdfTableSB = null;

    /**
     * Default title for cancer summary reports.
     */
    private static String TITLE = "Cancer Summary";

    /**
     * Default constructor.
     */
    public Service() {
        super();
    }

    /**
     * Generates a comprehensive cancer summary report for a specific respondent.
     * <p>
     * This endpoint creates a detailed cancer summary that:
     * <ul>
     *   <li>Aggregates cancer history across all family members</li>
     *   <li>Organizes information by cancer type</li>
     *   <li>Shows family member relationships and ages at diagnosis</li>
     *   <li>Indicates multiple cancer occurrences</li>
     *   <li>Provides both HTML and PDF format outputs</li>
     * </ul>
     *
     * @param req the report request containing the respondent ID
     * @return a ReportResponse containing the title, HTML content, and PDF document
     * @throws jakarta.ws.rs.WebApplicationException if the user lacks proper authorization
     */
    @Path("/report")
    @POST
    @RolesAllowed("cssummary-user")
    @Consumes("application/json")
    @Produces("application/json")
    @Transactional
    public ReportResponse report(ReportRequest req) {

        Table table = new Table();
        table.headers = new String[3];
        table.headers[0] = "Person";
        table.headers[1] = "Cancer";
        table.headers[2] = "Age";
        table.widths = new float[3];
        table.widths[0] = 100f;
        table.widths[1] = 300f;
        table.widths[2] = 100f;

        List<Row> pdfRows = new ArrayList<Row>();

        pdfTableSB = new StringBuffer();

        LinkedHashMap<String, Card> cards = new LinkedHashMap<String, Card>();
        String property = "";

        StringBuffer innerHTML = new StringBuffer();
        List<FactFHHSView> facts = FactFHHSView.find("respondent_id =?1 and step != 'Proband' ", Sort.by("relationship").and("step"), req.id).list();

        PDFDocument pdf = new PDFDocument();
        pdf.title = TITLE;
        pdf.styles = getPDFStyles();
        if (facts.size() > 0) {
            int i = 0;
            for (FactFHHSView f : facts) {
                List<Row> rows = f.getRows();
                if (rows.size() > 0) {
                    property = f.getTitle();
                    Card card = cards.get(property);
                    if (card == null) {
                        card = new Card(property);
                        cards.put(property, card);
                    }

                    pdfRows.addAll(rows);
                    card.addRows(rows);
                }
            }
            Set<Entry<String, Card>> cardSet = cards.entrySet();

            for (Entry<String, Card> entry : cardSet) {
                innerHTML.append(entry.getValue().getHTML());
            }


            table.body = new String[pdfRows.size()][3];
            for (Row row : pdfRows) {
                setPDFTableRow(row, table, i);
                i++;
            }

            pdf.content = getPDFContent(table);
        }
        if (cards.size() < 1) {
            innerHTML.append("No relatives reported cancer.");

            Content[] content = new Content[2];
            Content header = new Content();
            header.style = "ca_title";
            content[0] = header;

            Content body = new Content();
            body.text = "No relatives reported cancer.";
            content[1] = body;
            pdf.content = content;
        }

        return new ReportResponse("Cancer Summary", innerHTML.toString(), pdf);
    }

    /**
     * Sets a row in the PDF table with cancer data.
     *
     * @param row the row data to add
     * @param table the table to add the row to
     * @param index the row index
     */
    private void setPDFTableRow(Row row, Table table, int index) {
        table.body[index][0] = row.getTitle();
        table.body[index][1] = row.getLabel().replace("Age", "");
        table.body[index][2] = row.getValue();
    }

    /**
     * Gets the text value for display, handling null and blank values.
     *
     * @param value the value to process
     * @return formatted text value
     */
    private String getTextValue(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("true")) {
            return UKN_AGE;
        }
        return value;
    }

    /**
     * Configures and retrieves a map of predefined styles for formatting elements in a PDF document.
     * These styles are used to standardize the appearance of specific content types, such as titles
     * and report text, in the PDF generation process.
     * <p>
     * Each style entry in the map is defined by a string key and a corresponding Style object that
     * specifies various properties like font size, boldness, and margins.
     *
     * @return A map where the keys are style identifiers (e.g., "ca_title", "ca_bold"),
     * and the values are Style objects representing the styling configurations
     * for those identifiers.
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
     * Generates an array of {@link Content} objects to be included in a PDF document.
     * The method creates a predefined title content and processes the input table
     * into content suitable for PDF generation.
     *
     * @param table the {@link Table} object containing tabular data to be represented
     *              in the PDF, including headers, column widths, and body content.
     * @return an array of {@link Content} objects where the first element is a title
     * with predefined text and style, and the second element encapsulates
     * the provided table data.
     */
    public Content[] getPDFContent(Table table) {
        Content[] content = new Content[1];
        content[0] = new Content(table);
        return content;
    }
}
