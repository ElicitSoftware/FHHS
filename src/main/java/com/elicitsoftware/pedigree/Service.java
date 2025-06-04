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

@Path("pedigree")
@RequestScoped
public class Service {

    @Inject
    FamilyManager familyManager;

    @ConfigProperty(name = "pedigree.url")
    private String pedigreeURL;

    private static String TITLE = "Pedigree";

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

    @Path("/family")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("text/plain")
    @Transactional
    public String getFamily(ReportRequest req) {
        Family family = familyManager.getFamily(req.id);
        return family.toString();
    }

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
