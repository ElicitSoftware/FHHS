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

import com.elicitsoftware.common.security.ServiceToken;
import com.elicitsoftware.model.Family;
import com.elicitsoftware.model.FamilyManager;
import com.elicitsoftware.request.ReportRequest;
import com.elicitsoftware.response.ReportResponse;
import com.elicitsoftware.response.pdf.Content;
import com.elicitsoftware.response.pdf.PDFDocument;
import com.elicitsoftware.response.pdf.Style;
import jakarta.enterprise.context.RequestScoped;
import com.elicitsoftware.common.security.RespondentScope;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.logging.Log;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
         * HTTP client for external pedigree service communication.
         */
        private static final HttpClient PEDIGREE_HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            // UC-003 BR-003: the call carries the service token, so a redirect is never followed.
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /**
     * Default constructor for CDI instantiation.
     * <p>
     * No initialization logic; dependencies are injected by the container.
     */
    public Service() {
        // Default constructor for CDI
    }

    /**
     * Injected FamilyManager for accessing family data and relationships.
     * <p>
     * Used to retrieve family structure and member information for pedigree generation.
     */
    @Inject
    FamilyManager familyManager;

    /** Lets through only a respondent of the Family History Survey (UC-006 BR-009). */
    @Inject
    RespondentScope respondentScope;

    /**
     * Injected OpenTelemetry tracer for distributed tracing.
     * <p>
     * Used to trace pedigree report generation and service calls.
     */
    @Inject
    Tracer tracer;

    /**
     * The platform service token, presented to the pedigree service on every call (UC-003 BR-003).
     */
    @Inject
    ServiceToken serviceToken;

    /**
     * URL for the external pedigree generation service.
     * <p>
     * Configured via the "pedigree.url" application property.
     * Used for remote SVG pedigree diagram generation.
     */
    @ConfigProperty(name = "pedigree.url")
    private String pedigreeURL;

    /**
     * Default title for pedigree reports.
     * <p>
     * Used as the report heading and PDF title.
     */
    private static String TITLE = "Pedigree";

    /**
     * UC-003 BR-001: what the report shows in place of the diagram, in the HTML and in the PDF.
     * The cause is logged and recorded on the span, never placed in the report.
     */
    static final String DIAGRAM_UNAVAILABLE = "Pedigree diagram unavailable.";

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
     */
    @Path("/report")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json")
    @Transactional()
    public ReportResponse report(@Valid @NotNull ReportRequest req) {
        respondentScope.require("/pedigree/report", req.id);
        Log.debugf("Generating pedigree report for respondent id=%d", req.id);

        Family family = familyManager.getFamily(req.id);
        Log.debugf("Loaded family data for respondent id=%d", req.id);

        String response = callPedigree(family.toString(), family.family.size());

        // UC-003 A1: no answer from the drawing service leaves the fixed placeholder.
        String innerHTML = response != null ? response : DIAGRAM_UNAVAILABLE;

        PDFDocument pdf = new PDFDocument();
        pdf.pageBreak = true;
        pdf.landscape = true;
        pdf.title = TITLE;
        pdf.styles = getPDFStyles();
        pdf.content = getPDFContent(innerHTML, family.hasMultipleCancers());

        Log.debugf("Completed pedigree report generation for respondent id=%d", req.id);
        return new ReportResponse("Patient Pedigree", innerHTML, pdf);

    }

    /**
     * Calls the external pedigree service to generate the pedigree chart.
     * <p>
     * This method handles the communication with the external service, including
     * sending the family data and receiving the generated pedigree chart in SVG format.
     * </p>
     *
     * @param family the string representation of the family data
     * @param rows the number of family members in it, for the log (UC-003 BR-002)
     * @return the service's answer, or {@code null} when the call failed (UC-003 A1)
     */
    private String callPedigree(String family, int rows) {
        // UC-003 BR-003: pedigree.url is deployment configuration, so the token always goes with
        // the call; without a token the call is not made at all (UC-006 A3).
        String authorization = serviceToken.authorization().orElse(null);
        if (authorization == null) {
            Log.warnf("The pedigree service was not called: %s", ServiceToken.NOT_CONFIGURED);
            return null;
        }

        URI endpoint = URI.create(pedigreeURL);
        int endpointPort = resolvePort(endpoint);

        Span span = tracer.spanBuilder("pedigree.call")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        span.setAttribute("server.address", endpoint.getHost() == null ? pedigreeURL : endpoint.getHost());
        if (endpointPort > 0) {
            span.setAttribute("server.port", endpointPort);
        }
        span.setAttribute("url.full", pedigreeURL);
        span.setAttribute("http.request.method", "POST");
        span.setAttribute("rpc.system", "http");
        span.setAttribute("pedigree.payload.length", family.length());

        try (Scope ignored = span.makeCurrent()) {
            // UC-003 BR-002: the table is sent, not recorded - its size is.
            Log.infof("Calling the pedigree service at %s (%d rows, %d characters)", pedigreeURL, rows, family.length());

            String boundary = "----ElicitBoundary" + UUID.randomUUID();
            byte[] requestBody = buildMultipartBody(boundary, "ped", family);

            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    // Sent, never recorded: the span and the log carry no request header.
                    .header("Authorization", authorization)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();

            HttpResponse<String> response = PEDIGREE_HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            int statusCode = response.statusCode();
            Log.infof("Pedigree service returned status %d", statusCode);
            span.setAttribute("http.response.status_code", statusCode);
            if (statusCode == 200 || statusCode == 201) {
                span.setStatus(StatusCode.OK);
                return response.body();
            }

            span.setStatus(StatusCode.ERROR, "Server returned non-OK status: " + statusCode);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Interrupted while calling pedigree service");
            Log.warn("Interrupted while calling the pedigree service");
            return null;
        } catch (IOException e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            Log.warnf("The pedigree service could not be reached: %s", e.getMessage());
            return null;
        } finally {
            span.end();
        }
    }

    /**
     * Builds a multipart/form-data request body for HTTP POST.
     * <p>
     * Used to send family data to the external pedigree service.
     * </p>
     * @param boundary the multipart boundary string
     * @param fieldName the form field name
     * @param value the field value (family data)
     * @return byte array representing the multipart body
     */
    private byte[] buildMultipartBody(String boundary, String fieldName, String value) {
        StringBuilder body = new StringBuilder();
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"")
                .append(fieldName)
                .append("\"; filename=\"")
                .append(fieldName)
                .append("\"\r\n");
        body.append("Content-Type: text/plain\r\n");
        body.append("Content-Transfer-Encoding: binary\r\n\r\n");
        body.append(value).append("\r\n");
        body.append("--").append(boundary).append("--\r\n");
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Resolves the port number from a URI endpoint.
     * <p>
     * Returns the port if specified, otherwise returns the default port for the scheme (443 for HTTPS, 80 for HTTP).
     * Returns -1 if the scheme is unknown or no port is specified.
     *
     * @param endpoint the URI endpoint to resolve
     * @return the port number, or -1 if not found
     */
    private int resolvePort(URI endpoint) {
        if (endpoint.getPort() > 0) {
            return endpoint.getPort();
        }
        String scheme = endpoint.getScheme();
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        return -1;
    }

    /**
     * Checks if the provided content is valid SVG.
     * <p>
     * Valid SVG content should start with an XML declaration or an SVG tag.
     * This method is used to validate responses from the pedigree service
     * before attempting to parse them as SVG.
     *
     * @param content the content to check
     * @return true if the content appears to be valid SVG, false otherwise
     */
    private boolean isValidSvg(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        String trimmed = content.trim();
        return trimmed.startsWith("<?xml") || trimmed.startsWith("<svg") || trimmed.startsWith("<SVG");
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
     * <p>
     * If the SVG content is invalid (e.g., an error message from the pedigree service),
     * the method shows the fixed placeholder in the PDF instead of failing (UC-003 BR-001).
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
        // Validate SVG content before setting it - if invalid, show error text instead
        if (isValidSvg(svg)) {
            svgContent.svg = svg;
        } else {
            // The pedigree service returned an error or invalid response; its text is not repeated
            svgContent.text = DIAGRAM_UNAVAILABLE;
        }
        content[0] = svgContent;

        Content red = new Content();
        red.text = "red fill = family member with cancer";
        red.style = "ped_red";
        content[1] = red;

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
