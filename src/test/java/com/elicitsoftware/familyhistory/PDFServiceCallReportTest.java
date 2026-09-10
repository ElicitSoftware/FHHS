package com.elicitsoftware.familyhistory;

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

import com.elicitsoftware.model.ReportDefinition;
import com.elicitsoftware.response.ReportResponse;
import com.elicitsoftware.test.PostgresTestResource;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@code PDFService#callReport(ReportDefinition, long)} - the piece of
 * {@code generatePDF(long)} that implements BR-004 (invoking each report configured for the
 * respondent's survey). It builds its own MicroProfile REST client per call rather than an
 * injected one, so it is driven here against a real (but tiny, in-process) HTTP server rather
 * than a mock, and invoked via reflection since it is private. {@code RestClientBuilder.newBuilder()}
 * needs Quarkus's MicroProfile Rest Client resolver, registered process-wide only once Quarkus has
 * booted, so this must run as a {@code @QuarkusTest} even though {@code PDFService} itself is
 * plain-instantiated (not injected) - without it, {@code newBuilder()} fails immediately with
 * "No RestClientBuilderResolver implementation found!", which callReport's own generic catch
 * block then reports as if it were a real connection failure.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class PDFServiceCallReportTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private String startServer(int status, String contentType, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            if (contentType != null) {
                exchange.getResponseHeaders().add("Content-Type", contentType);
            }
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    private ReportResponse invokeCallReport(String url, long respondentId) throws Exception {
        ReportDefinition rpt = new ReportDefinition();
        rpt.name = "Cancer Summary";
        rpt.url = url;

        Method method = PDFService.class.getDeclaredMethod("callReport", ReportDefinition.class, long.class);
        method.setAccessible(true);
        try {
            return (ReportResponse) method.invoke(new PDFService(), rpt, respondentId);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }

    @Test
    void callReport_successfulResponse_returnsItDeserializedAsIs() throws Exception {
        String url = startServer(200, "application/json",
                "{\"title\":\"Cancer Summary\",\"innerHTML\":\"<p>ok</p>\"}");

        ReportResponse response = invokeCallReport(url, 42L);

        assertEquals("Cancer Summary", response.title);
        assertEquals("<p>ok</p>", response.innerHTML);
        assertNull(response.pdf);
    }

    @Test
    void callReport_errorResponseWithEntityBody_usesResponseBodyAsErrorMessage() throws Exception {
        String url = startServer(500, "text/plain", "database connection refused");

        ReportResponse response = invokeCallReport(url, 42L);

        assertEquals("Error: Cancer Summary", response.title);
        assertEquals("database connection refused", response.innerHTML);
        assertNotNull(response.pdf);
        assertEquals("Error: Cancer Summary", response.pdf.title);
        assertEquals(1, response.pdf.content.length);
        assertTrue(response.pdf.content[0].text.contains("database connection refused"));
    }

    @Test
    void callReport_forbiddenResponseWithoutEntity_usesLicenseErrorMessage() throws Exception {
        String url = startServer(403, null, "");

        ReportResponse response = invokeCallReport(url, 42L);

        assertEquals("Error: Cancer Summary", response.title);
        assertTrue(response.innerHTML.contains("Access forbidden") || response.innerHTML.contains("HTTP 403"),
                "expected a 403-specific message, got: " + response.innerHTML);
    }

    @Test
    void callReport_unreachableOrMalformedUrl_fallsBackToGenericErrorPath() throws Exception {
        // new URI(rpt.url) throws URISyntaxException before any network call - no server needed,
        // and it is caught by callReport's generic catch (Exception e) branch just like a real
        // connection failure would be.
        ReportResponse response = invokeCallReport("http://[invalid", 42L);

        assertEquals("Error: Cancer Summary", response.title);
        assertNotNull(response.pdf);
        assertTrue(response.pdf.content[0].text.startsWith("Error generating Cancer Summary report:"));
    }
}
