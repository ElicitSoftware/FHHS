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

import com.elicitsoftware.common.security.ServiceToken;
import com.elicitsoftware.model.ReportDefinition;
import com.elicitsoftware.response.ReportResponse;
import com.elicitsoftware.test.LogCapture;
import com.elicitsoftware.test.PostgresTestResource;
import com.elicitsoftware.test.ServiceCalls;
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
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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

    /** What the stand-in was last sent as its Authorization header, and how often it was called. */
    private volatile String presentedAuthorization;
    private final AtomicInteger calls = new AtomicInteger();

    /** The token the service under test holds; by default one that trusts the stand-in's origin. */
    private ServiceToken serviceToken;

    /** The read timeout the service under test uses; null leaves the one it ships with. */
    private Duration readTimeout;

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
            presentedAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            calls.incrementAndGet();
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
        if (serviceToken == null) {
            serviceToken = ServiceCalls.serviceToken("http://localhost:" + server.getAddress().getPort());
        }
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    private ReportResponse invokeCallReport(String url, long respondentId) throws Exception {
        ReportDefinition rpt = new ReportDefinition();
        rpt.name = "Cancer Summary";
        rpt.url = url;

        Method method = PDFService.class.getDeclaredMethod("callReport", ReportDefinition.class, long.class);
        method.setAccessible(true);
        try {
            PDFService service = new PDFService();
            service.serviceToken = serviceToken != null ? serviceToken : ServiceCalls.serviceToken();
            if (readTimeout != null) {
                service.reportReadTimeout = readTimeout;
            }
            return (ReportResponse) method.invoke(service, rpt, respondentId);
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

    // UC-004 BR-004, NFR-010: the section names the report and the status, never what the service answered.
    @Test
    void callReport_errorResponseWithEntityBody_givesTheStatusNotTheBody() throws Exception {
        String url = startServer(500, "text/plain", "database connection refused at db.internal:5432");

        try (LogCapture log = LogCapture.start()) {
            ReportResponse response = invokeCallReport(url, 42L);

            assertEquals("Error: Cancer Summary", response.title);
            assertEquals("The Cancer Summary report could not be generated (HTTP 500).", response.innerHTML);
            assertNotNull(response.pdf);
            assertEquals("Error: Cancer Summary", response.pdf.title);
            assertEquals(1, response.pdf.content.length);
            assertEquals("The Cancer Summary report could not be generated (HTTP 500).", response.pdf.content[0].text);
            assertFalse(log.contains("db.internal"), "NFR-009: the service's answer is not logged either");
        }
    }

    @Test
    void callReport_forbiddenResponseWithoutEntity_givesTheStatus() throws Exception {
        String url = startServer(403, null, "");

        ReportResponse response = invokeCallReport(url, 42L);

        assertEquals("Error: Cancer Summary", response.title);
        assertEquals("The Cancer Summary report could not be generated (HTTP 403).", response.innerHTML);
    }

    @Test
    void callReport_unreachableOrMalformedUrl_fallsBackToGenericErrorPath() throws Exception {
        // new URI(rpt.url) throws URISyntaxException before any network call - no server needed,
        // and it is caught by callReport's generic catch (Exception e) branch just like a real
        // connection failure would be.
        ReportResponse response = invokeCallReport("http://[invalid", 42L);

        assertEquals("Error: Cancer Summary", response.title);
        assertNotNull(response.pdf);
        assertEquals("The Cancer Summary report could not be generated (the service did not answer).",
                response.pdf.content[0].text);
        assertFalse(response.innerHTML.contains("invalid"), "NFR-010: no exception message reaches the report");
    }

    private static final String OK_BODY = "{\"title\":\"Cancer Summary\",\"innerHTML\":\"<p>ok</p>\"}";

    /** UC-004 BR-004, UC-006 BR-006: a report at a trusted origin is sent the service token. */
    @Test
    void callReport_trustedOrigin_presentsTheServiceToken() throws Exception {
        String url = startServer(200, "application/json", OK_BODY);

        ReportResponse response = invokeCallReport(url, 42L);

        assertEquals("Bearer " + ServiceCalls.TOKEN, presentedAuthorization);
        assertEquals("<p>ok</p>", response.innerHTML);
    }

    /**
     * UC-006 A7, BR-006: an address whose origin the deployment did not list is called without
     * the token, however the definition spells it. Here the listed origin is the same host on
     * another port.
     */
    @Test
    void callReport_originNotTrusted_sendsNoToken() throws Exception {
        serviceToken = ServiceCalls.serviceToken("http://localhost:1");
        String url = startServer(200, "application/json", OK_BODY);

        try (LogCapture log = LogCapture.start()) {
            invokeCallReport(url, 42L);

            assertEquals(1, calls.get());
            assertNull(presentedAuthorization, "the token must not reach an origin that is not trusted");
            assertTrue(log.contains("ELICIT_SERVICE_TRUSTED_ORIGINS"), log.text());
            assertFalse(log.contains(ServiceCalls.TOKEN), log.text());
        }
    }

    /** UC-006 A3: with no token configured the call is not made, and the report's section says why. */
    @Test
    void callReport_withoutAServiceToken_makesNoCall() throws Exception {
        serviceToken = ServiceCalls.serviceTokenNotConfigured();
        String url = startServer(200, "application/json", OK_BODY);

        ReportResponse response = invokeCallReport(url, 42L);

        assertEquals(0, calls.get(), "no request may leave without the token");
        assertEquals("Error: Cancer Summary", response.title);
        assertTrue(response.innerHTML.contains("service token not configured"), response.innerHTML);
    }

    /** UC-004 BR-004: a redirect is not followed, so the token cannot be carried to another address. */
    @Test
    void callReport_redirect_isNotFollowed() throws Exception {
        String elsewhere = startServer(200, "application/json", OK_BODY);
        HttpServer target = server;
        HttpServer redirecting = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        redirecting.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Location", elsewhere);
            exchange.sendResponseHeaders(307, -1);
            exchange.close();
        });
        redirecting.start();
        try {
            serviceToken = ServiceCalls.serviceToken("http://localhost:" + redirecting.getAddress().getPort());

            ReportResponse response = invokeCallReport("http://localhost:" + redirecting.getAddress().getPort() + "/", 42L);

            assertEquals(0, calls.get(), "the redirect target must not be called");
            assertNotEquals("<p>ok</p>", response.innerHTML);
        } finally {
            redirecting.stop(0);
            target.stop(0);
        }
    }

    /** UC-006 BR-004: the token is sent, and written to no log record at any level, also when the call fails. */
    @Test
    void callReport_doesNotLogTheServiceToken() throws Exception {
        String url = startServer(500, "text/plain", "refused");

        try (LogCapture log = LogCapture.start()) {
            ReportResponse response = invokeCallReport(url, 42L);

            assertEquals("Bearer " + ServiceCalls.TOKEN, presentedAuthorization);
            assertFalse(log.contains(ServiceCalls.TOKEN), log.text());
            assertFalse(response.innerHTML.contains(ServiceCalls.TOKEN));
            assertFalse(response.pdf.content[0].text.contains(ServiceCalls.TOKEN));
        }
    }

    /**
     * UC-004 BR-004, NFR-013: a report service that accepts the request and never answers is
     * given up on at the read timeout, and its section carries the error like any report that
     * could not be obtained.
     */
    @Test
    void callReport_serviceNeverAnswers_givesUpAtTheReadTimeout() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
        String origin = "http://localhost:" + server.getAddress().getPort();
        serviceToken = ServiceCalls.serviceToken(origin);
        readTimeout = Duration.ofMillis(500);

        try {
            ReportResponse response = assertTimeoutPreemptively(Duration.ofSeconds(20),
                    () -> invokeCallReport(origin + "/", 42L));

            assertEquals(1, calls.get());
            assertEquals("Error: Cancer Summary", response.title);
            assertNotNull(response.pdf);
            assertEquals("The Cancer Summary report could not be generated (the service did not answer).",
                    response.pdf.content[0].text);
        } finally {
            release.countDown();
        }
    }

    /** NFR-013: the bounds a delivery runs with are the ones Survey puts on the same call. */
    @Test
    void callReport_shipsWithTheSpecifiedBounds() {
        assertEquals(Duration.ofSeconds(5), PDFService.REPORT_CONNECT_TIMEOUT);
        assertEquals(Duration.ofSeconds(60), PDFService.REPORT_READ_TIMEOUT);
        assertEquals(PDFService.REPORT_READ_TIMEOUT, new PDFService().reportReadTimeout);
    }
}
