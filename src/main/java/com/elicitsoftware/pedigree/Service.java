package com.elicitsoftware.pedigree;

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

import com.elicitsoftware.model.Family;
import com.elicitsoftware.model.FamilyManager;
import com.elicitsoftware.request.ReportRequest;
import com.elicitsoftware.response.ReportResponse;
import com.elicitsoftware.response.pdf.Content;
import com.elicitsoftware.response.pdf.PDFDocument;
import com.elicitsoftware.response.pdf.Style;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST service for generating family pedigree reports and visualizations.
 * <p>
 * This service creates comprehensive family pedigree charts showing family
 * relationships and cancer history. It integrates with external pedigree
 * generation services to create visual family trees and generates both
 * HTML and PDF format reports with color-coded cancer indicators.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@Path("pedigree")
@RequestScoped
public class Service {

    /**
     * Default constructor.
     */
    public Service() {
        // Default constructor for CDI
    }

    /**
     * Injected FamilyManager for accessing family data and relationships.
     */
    @Inject
    FamilyManager familyManager;

    /**
     * URL for the external pedigree generation service.
     * Configured via the "pedigree.url" application property.
     */
    @ConfigProperty(name = "pedigree.url")
    private String pedigreeURL;

    /**
     * Default title for pedigree reports.
     */
    private static String TITLE = "Pedigree";

    /**
     * Generates a comprehensive pedigree report including family tree visualization.
     * <p>
     * This endpoint creates a detailed family pedigree chart showing:
     * <ul>
     *   <li>Family relationships and structure</li>
     *   <li>Cancer history indicators (color-coded)</li>
     *   <li>Multiple cancer diagnosis markers</li>
     *   <li>Visual legend explaining the color coding</li>
     * </ul>
     *
     * @param req the report request containing the respondent ID
     * @return a ReportResponse containing the title, HTML content, and PDF document
     * @throws jakarta.ws.rs.WebApplicationException if the user lacks proper authorization
     */
    @Path("/report")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json")
    @Transactional()
    public ReportResponse report(ReportRequest req) {

        Family family = familyManager.getFamily(req.id);

        String response = callPedigree(family.toString());

        String innerHTML = response;

        PDFDocument pdf = new PDFDocument();
        pdf.pageBreak = true;
        pdf.landscape = true;
        pdf.title = TITLE;
        pdf.styles = getPDFStyles();
        pdf.content = getPDFContent(innerHTML, family.hasMultipleCancers());

        return new ReportResponse("Patient Pedigree", innerHTML, pdf);

    }

    /**
     * Retrieves family information in a textual format for the specified report request.
     * <p>
     * This endpoint returns a string representation of the family's cancer history and
     * relationships for the given respondent ID. It is primarily used for debugging
     * and verification purposes.
     * </p>
     *
     * @param req the report request containing the respondent ID
     * @return a string representation of the family data
     */
    @Path("/family")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("text/plain")
    @Transactional
    public String getFamily(ReportRequest req) {
        Family family = familyManager.getFamily(req.id);
        return family.toString();
    }

    /**
     * Calls the external pedigree service to generate the pedigree chart.
     * <p>
     * This method handles the communication with the external service, including
     * sending the family data and receiving the generated pedigree chart in SVG format.
     * </p>
     *
     * @param family the string representation of the family data
     * @return the SVG content of the generated pedigree chart
     */
    private String callPedigree(String family) {

        MultipartUtility multipart;
        List<String> response;
        try {
            multipart = new MultipartUtility(this.pedigreeURL, "UTF-8");
            multipart.addFilePart("ped", family);
            response = multipart.finish();
            return String.join("",response);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return e.getMessage();
        }
    }

    /**
     * Creates the content array for the PDF document including SVG and legend elements.
     * <p>
     * This method assembles the various content elements for the PDF:
     * <ul>
     *   <li>SVG pedigree chart</li>
     *   <li>Color legend for respondent identification</li>
     *   <li>Color legend for cancer indicators</li>
     *   <li>Multiple diagnosis disclaimer (if applicable)</li>
     * </ul>
     *
     * @param svg the SVG content representing the family pedigree
     * @param multipleCancers indicates whether any family members have multiple cancer diagnoses
     * @return array of Content objects for PDF generation
     */
    private Content[] getPDFContent(String svg, boolean multipleCancers) {
        Content[] content;

        if (!multipleCancers) {
            content = new Content[3];
        } else {
            content = new Content[4];
            Content multiple = new Content();
            multiple.text = "* Indicates multiple diagnoses of the same cancer type.";
            content[3] = multiple;
        }

        Content svgContent = new Content();
        svgContent.svg = svg;
        content[0] = svgContent;

        Content green = new Content();
        green.text = "green = respondent";
        green.style = "ped_green";
        content[1] = green;

        Content red = new Content();
        red.text = "red = family member with cancer";
        red.style = "ped_red";
        content[2] = red;

        return content;

    }

    /**
     * Creates a map of style configurations for pedigree PDF formatting.
     * <p>
     * Defines custom styles for different elements in the pedigree PDF:
     * <ul>
     *   <li>"ped_title": Centered, bold title with larger font size</li>
     *   <li>"ped_image": Center-aligned style for SVG content</li>
     *   <li>"ped_green": Green text color for respondent identification</li>
     *   <li>"ped_red": Red text color for cancer indicators</li>
     * </ul>
     *
     * @return a map containing style configurations for pedigree elements
     */
    private Map<String, Style> getPDFStyles() {
        HashMap<String, Style> styles = new HashMap<String, Style>();
        Style title = new Style();
        title.fontSize = 16;
        title.alignment = "center";
        title.bold = true;
        title.margin = new Integer[]{0, 20, 0, 0};
        styles.put("ped_title", title);

        Style image = new Style();
        image.alignment = "center";
        styles.put("ped_image", image);

        Style green = new Style();
        green.color = "green";
        styles.put("ped_green", green);

        Style red = new Style();
        red.color = "red";
        styles.put("ped_red", red);
        return styles;

    }
}
