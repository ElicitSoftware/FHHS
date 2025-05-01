package com.elicitsoftware.premm5;

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

import com.elicitsoftware.model.FactFHHSView;
import com.elicitsoftware.premm5.response.CalculateResponse;
import com.elicitsoftware.request.ReportRequest;
import com.elicitsoftware.response.ReportResponse;
import com.elicitsoftware.response.pdf.Content;
import com.elicitsoftware.response.pdf.PDFDocument;
import com.elicitsoftware.response.pdf.Style;
import io.quarkus.panache.common.Sort;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/premm5")
@RequestScoped
public class Service {

    public static final String TRUE = "true";
    private final DecimalFormat df = new DecimalFormat("##.00");
    private static String TITLE = "Premm5 Score";

    @Path("/report")
    @POST
    @RolesAllowed("premm5-user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json")
    @Transactional
    public ReportResponse report(ReportRequest req) {

        String premm5Score = "0";
        String premm5Warn = """
            <style>
                .premm5Warn {
                    color: red;
                }
            </style> 
            """;

        StringBuffer innerHTML = new StringBuffer( premm5Warn + "<div class=\"card\">");

        innerHTML.append("\n<div class=\"card--content \">");
        innerHTML.append("\n<span class=\"card--content--label\">");
        innerHTML.append(" PREMM5 Score \n\n");
        innerHTML.append("\n</span><span class=\"card--content--response");

        String[] pdfContent = new String[4];

        PDFDocument pdf = new PDFDocument();
        pdf.title = TITLE;
        pdf.styles = getPDFStyles();

        Premm5View view = getPremm5View(req.id);

        if (view.familyHasCancerDX()) {
            Premm5 premm5 = new Premm5(view.getCalculateRequest());

            CalculateResponse calculateResponse = premm5.calculate();
            if (calculateResponse.getAny() >= 5) {
                innerHTML.append(" premm5Warn");
            }
            premm5Score = calculateResponse.getAnyRounded();

            if (calculateResponse.getAny() >= 50) {
                innerHTML.append("\">" + "≥ 50%</br>");
            } else {
                innerHTML.append("\">" + premm5Score + "%</br>");
            }
            innerHTML.append("<p>The PREMM5 model is a clinical prediction algorithm that estimates the cumulative probability "
                    + "of an individual carrying a germline mutation in the MLH1, MSH2, MSH6, PMS2, or EPCAM genes. "
                    + "Mutations in these genes cause Lynch syndrome, an inherited cancer predisposition syndrome.</p>");
            innerHTML.append("\n<p>* PREMM scores are based on initial patient survey input and for preliminary risk assessment purposes only.</p>");

            pdfContent[0] = premm5Score + "%";
            pdfContent[1] = "The PREMM5 model is a clinical prediction algorithm that estimates the cumulative probability "
                    + "of an individual carrying a germline mutation in the MLH1, MSH2, MSH6, PMS2, or EPCAM genes. "
                    + "Mutations in these genes cause Lynch syndrome, an inherited cancer predisposition syndrome.";
            pdfContent[2] = "PREMM scores are based on initial patient survey input and for preliminary risk assessment purposes only.";
            pdfContent[3] = "Family history and PREMM scores should be confirmed (https://premm.dfci.harvard.edu) prior to use in clinical care.";

            pdf.content = getPDFContent(pdfContent, calculateResponse.getAny() >= 2.5);

        } else {
            innerHTML.append(" premm5Warn");
            innerHTML.append("\"> PREMM is not designed for risk assessment in the absence of relevant personal and family history of cancer. Please see your healthcare provider for specific recommendations.");

            Content[] content = new Content[1];
            Content body = new Content();
            body.text = "PREMM is not designed for risk assessment in the absence of relevant personal and family history of cancer. Please see your healthcare provider for specific recommendations.";
            content[0] = body;
            pdf.content = content;
        }
        innerHTML.append("</span><br/></div><mat-divider></mat-divider></div>");

        return new ReportResponse("Premm 5 Score", innerHTML.toString(), pdf);
    }

    private Premm5View getPremm5View(long id) {
        Premm5View view = new Premm5View();

        FactFHHSView probandView = FactFHHSView.find("respondent_id =?1 and step = 'Proband'", id).firstResult();
        List<FactFHHSView> firstDegreeViews = FactFHHSView.find("respondent_id =?1 and relationship = 1 ", Sort.by("relationship").and("step"), id).list();
        List<FactFHHSView> secondDegreeViews = FactFHHSView.find("respondent_id =?1 and relationship = 2 ", Sort.by("relationship").and("step"), id).list();

        view.respondentId = id;
        view.gender = probandView.gender;
        view.age = probandView.age;

        if (TRUE.equalsIgnoreCase(probandView.colon_or_rectal_cancer)) {
            view.CRC = "true";
        }
        if(TRUE.equalsIgnoreCase(probandView.multiple_colon_or_rectal_cancers)){
            view.CRCMultiple = true;
        }

        view.crcMinAge = probandView.colon_or_rectal_cancer_age;

        if (TRUE.equalsIgnoreCase(probandView.endometrial_or_uterine_cancer)) {
            view.ec = "true";
        }
        view.ecMinAge = probandView.endometrial_or_uterine_cancer_age;

        Integer ls = lynchSyndromeCount(new ArrayList<FactFHHSView>(List.of(probandView)));
        if (ls != null && ls > 0) {
            view.ls = "true";
        }

        //First Degree Relatives
        view.fdrCrc = crcCount(firstDegreeViews);
        view.fdrMinCrc = minCRC(firstDegreeViews);
        view.fdrEc = ecCount(firstDegreeViews);
        view.fdrMinEc = minEC(firstDegreeViews);
        view.fdrLS = lynchSyndromeCount(firstDegreeViews);

        //Second Degree Relatives
        view.sdrCrc = crcCount(secondDegreeViews);
        view.sdrMinCrc = minCRC(secondDegreeViews);
        view.sdrEc = ecCount(secondDegreeViews);
        view.sdrMinEc = minEC(secondDegreeViews);
        view.sdrLs = lynchSyndromeCount(secondDegreeViews);

        return view;
    }

    private Integer crcCount(List<FactFHHSView> facts) {
        Integer crc = 0;
        for (FactFHHSView fact : facts) {
            if (TRUE.equalsIgnoreCase(fact.colon_or_rectal_cancer)) {
                crc++;
            }
        }
        if (crc == 0) {
            return null;
        } else {
            return crc;
        }
    }

    /**
     * Determines the minimum non-null colon or rectal cancer age from the given list of facts.
     * If no valid colon or rectal cancer age is found, the method returns null.
     *
     * @param facts a list of FactFHHSView objects, each containing information on colon or rectal cancer age.
     * @return the minimum non-null colon or rectal cancer age as an Integer, or null if no valid age exists.
     */
    private Integer minCRC(List<FactFHHSView> facts) {
        // Check the default age.
        Integer minCRC = 1000;
        for (FactFHHSView fact : facts) {
            if (fact.colon_or_rectal_cancer_age != null) {
                if (minCRC > fact.colon_or_rectal_cancer_age)
                    minCRC = fact.colon_or_rectal_cancer_age;
            }
        }
        if (minCRC == 1000) {
            return null;
        } else {
            return minCRC;
        }
    }

    /**
     * Calculates the count of entries in the given list where the value of the
     * 'endometrial_or_uterine_cancer' property matches a specific condition.
     * If no such entries are found, returns null.
     *
     * @param facts A list of FactFHHSView objects to evaluate.
     *              Each object represents a data entry containing information
     *              about endometrial or uterine cancer occurrences.
     * @return The count of entries satisfying the condition, or null if no
     * entries meet the condition.
     */
    private Integer ecCount(List<FactFHHSView> facts) {
        Integer ec = 0;
        for (FactFHHSView fact : facts) {
            if (TRUE.equalsIgnoreCase(fact.endometrial_or_uterine_cancer)) {
                ec++;
            }
        }
        if (ec == 0) {
            return null;
        } else {
            return ec;
        }
    }

    /**
     * Determines the minimum age at which endometrial or uterine cancer was reported from a list of facts.
     * If no such age is present, returns null.
     *
     * @param facts A list of FactFHHSView objects, each containing information about endometrial or uterine cancer age.
     * @return The minimum age as an Integer, or null if no age values are present in the input list.
     */
    private Integer minEC(List<FactFHHSView> facts) {
        // Check the default age.
        Integer minEC = 1000;
        for (FactFHHSView fact : facts) {
            if (fact.endometrial_or_uterine_cancer_age != null) {
                if (minEC > fact.endometrial_or_uterine_cancer_age)
                    minEC = fact.endometrial_or_uterine_cancer_age;
            }
        }
        if (minEC == 1000) {
            return null;
        } else {
            return minEC;
        }
    }

    /**
     * Calculates the count of Lynch Syndrome cases based on the input list of FactFHHSView objects.
     * Lynch Syndrome is identified based on specific cancers or the presence of certain keywords
     * in the "other cancer name" field.
     *
     * @param facts a list of FactFHHSView objects containing cancer data for evaluation
     * @return the count of Lynch Syndrome cases as an Integer, or null if no cases are found
     */
    private Integer lynchSyndromeCount(List<FactFHHSView> facts) {
//        Lynch Syndrom is defined as having one of these cancers.
//        bladder_cancer, stomach_cancer, kidney_renal_cell_cancer, ovarian_cancer, pancreatic_cancer
//        or one of these in other cancer
//        'Ovary|Fallopian|Stomach|Gastric|Intestine|Duodenum|Bowel|Jejunum|Ileum|Urinary|Bladder|Kidney|Renal|Bile|Cholangiocarcinoma|Brain|Glioblastoma|Sebaceous'
        Integer ls = 0;
        for (FactFHHSView fact : facts) {
            if ((TRUE.equalsIgnoreCase(fact.bladder_cancer))
                    || (TRUE.equalsIgnoreCase(fact.stomach_cancer))
                    || (TRUE.equalsIgnoreCase(fact.kidney_renal_cell_cancer))
                    || (TRUE.equalsIgnoreCase(fact.ovarian_cancer))
                    || (TRUE.equalsIgnoreCase(fact.pancreatic_cancer))) {
                ls++;
            }

            if (fact.other_cancer_name != null && !fact.other_cancer_name.isEmpty()) {
                String regex = "bladder|bowel|brain|bile|cholangiocarcinoma|duodenum|fallopian|gastric|glioblastoma|ileum|intestine|jejunum|kidney|ovary|renal|sebaceous|stomach|urinary";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(fact.other_cancer_name.toLowerCase());
                if (matcher.find()) {
                    ls++;
                }
            }
        }

        if (ls == 0) {
            return null;
        } else {
            return ls;
        }
    }

    /**
     * Calculates and retrieves the Premm5 score based on the provided ID.
     * If a family history of cancer is found, the Premm5 score is computed
     * and returned as part of the response. If no cancer history is detected,
     * an appropriate message is returned.
     *
     * @param id The unique identifier for the record used to fetch the Premm5 view and calculate the score.
     * @return A JSON-formatted string containing either the computed Premm5 score or a message indicating no family history of cancer.
     */
    @Path("/score")
    @GET
    @RolesAllowed("premm5-user")
    @Produces("application/json")
    @Transactional
    public String report(@QueryParam(value = "id") Long id) {
        String premm5Score = "0";
        Premm5View view = null;

        try {
            view = getPremm5View(id);
        } catch (Exception e) {
            premm5Score = "-1";
        }
        if (view != null && view.familyHasCancerDX()) {
            Premm5 premm5 = new Premm5(view.getCalculateRequest());
            CalculateResponse calculateResponse = premm5.calculate();
            premm5Score = calculateResponse.getAnyRounded();
//            value = saveReportValue(1l, id, premm5Score);
//            value.persist();
            return "{\"msg\": \"Score Id was " + premm5Score + "\"}";
        } else {
//            value = saveReportValue(1l, id, premm5Score);
//            value.persist();
            return "{\"msg\": \"No family history of cancer \"}";
        }
    }

//    private ReportValue saveReportValue(Long surveyId, Long respondentId, String value) {
//        ReportValue reportValue = ReportValue.findByKeys(surveyId, respondentId);
//        if (reportValue != null) {
//            if (!reportValue.value.equals(value)) {
//                reportValue.value = value;
//            }
//        } else {
//            reportValue = new ReportValue(surveyId, respondentId, value);
//        }
//        return reportValue;
//    }

    /**
     * Generates an array of Content objects representing the information to be displayed in a PDF document.
     * This method combines a title and elements derived from the given response input, applying specific styles
     * based on the value of the isHigh parameter.
     *
     * @param response An array of strings representing the input text data to be included in the PDF content.
     * @param isHigh   A boolean flag that determines the style of the content. If true, the content uses a specific style (e.g., premm_body_red);
     *                 otherwise, a default style (e.g., premm_body) is applied.
     * @return An array of Content objects, where the first element includes a title and the subsequent elements include
     * the styled representations of the input response data.
     */
    public Content[] getPDFContent(String[] response, boolean isHigh) {

        int items = response.length;
        Content[] content = new Content[items];
        for (int i = 0; i < response.length; i++) {
            Content text = new Content();
            text.text = response[i];
            if (isHigh) {
                text.style = "premm_body_red";
            } else {
                text.style = "premm_body";
            }
            content[i] = text;
        }

        return content;

    }

    /**
     * Creates and returns a map of predefined styles used for formatting content in a PDF document.
     * Each style represents a specific set of formatting configurations, including font size,
     * color, boldness, and margins.
     * <p>
     * The returned map contains the following styles:
     * - "premm_title": A style for titles with a font size of 16, bold text, and specified margins.
     * - "premm_body": A style for body text with a font size of 12 and specified margins.
     * - "premm_body_red": A style for body text with a font size of 14, red color, and specified margins.
     *
     * @return A map where the keys are style names and the values are corresponding Style objects
     * containing the formatting configurations.
     */
    private Map<String, Style> getPDFStyles() {
        HashMap<String, Style> styles = new HashMap<String, Style>();
        Style title = new Style();
        title.fontSize = 16;
        title.bold = true;
        title.margin = new Integer[]{0, 40, 0, 10};
        styles.put("premm_title", title);

        Style body = new Style();
        body.fontSize = 12;
        body.margin = new Integer[]{0, 10, 0, 0};
        styles.put("premm_body", body);

        Style red = new Style();
        red.fontSize = 14;
        red.color = "red";
        red.margin = new Integer[]{0, 10, 0, 0};
        styles.put("premm_body_red", red);

        return styles;
    }
}
