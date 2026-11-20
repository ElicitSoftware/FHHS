package com.elicitsoftware.common.security;

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

import com.elicitsoftware.familyhistory.PDFService;
import com.elicitsoftware.model.CancerHistoryRepository;
import com.elicitsoftware.model.ReportDefinition;
import com.elicitsoftware.test.LogCapture;
import com.elicitsoftware.test.PostgresTestResource;
import com.elicitsoftware.test.RecordedSpans;
import com.elicitsoftware.test.ServiceCalls;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * UC-006 against the running application: every REST path requires the service token (BR-001),
 * the token is checked before the survey check (BR-005), the health probes stay open, and
 * neither the log nor a trace of a service call carries the token (BR-004).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ServiceTokenEndpointsTest {

    @InjectSpy
    CancerHistoryRepository cancerHistoryRepository;

    /** Accepts every respondent: these tests are about the token, RespondentScopeTest about the scope. */
    @InjectMock
    RespondentScope respondentScope;

    @Inject
    ServiceToken serviceToken;

    @Inject
    @Readiness
    ServiceTokenHealthCheck healthCheck;

    @Inject
    RecordedSpans recordedSpans;

    @Inject
    jakarta.persistence.EntityManager em;

    /** UC-006 A1, BR-001: no header is answered 401 on every path, and no respondent data is read. */
    @ParameterizedTest
    @ValueSource(strings = {"/proband/report", "/casummary/report", "/pedigree/report", "/familyhistory/generate",
            "/no/such/path", "/"})
    void withoutTheToken_everyPathAnswers401(String path) {
        clearInvocations(cancerHistoryRepository);

        given().contentType(ContentType.JSON).body("{\"id\":1}")
                .when().post(path)
                .then().statusCode(401)
                .header("WWW-Authenticate", equalTo("Bearer"))
                .body(equalTo(""));
        given().when().get(path).then().statusCode(401);

        verifyNoInteractions(cancerHistoryRepository);
    }

    /** UC-006 A2: a wrong token, and a credential of another scheme, get the same answer. */
    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong", "Bearer " + ServiceCalls.TOKEN + "x", "Basic " + ServiceCalls.TOKEN})
    void withAWrongCredential_theAnswerIsTheSame401(String authorization) {
        clearInvocations(cancerHistoryRepository);

        given().header("Authorization", authorization).contentType(ContentType.JSON).body("{\"id\":1}")
                .when().post("/proband/report")
                .then().statusCode(401)
                .header("WWW-Authenticate", equalTo("Bearer"))
                .body(equalTo(""));

        verifyNoInteractions(cancerHistoryRepository);
    }

    /** UC-006 main scenario: with the token the request reaches its use case. */
    @ParameterizedTest
    @ValueSource(strings = {"/proband/report", "/casummary/report"})
    void withTheToken_theReportIsServed(String path) {
        ServiceCalls.given().contentType(ContentType.JSON).body("{\"id\":999999}")
                .when().post(path)
                .then().statusCode(200);
    }

    /** UC-006 BR-001: with the token a path that does not exist is simply not found. */
    @Test
    void withTheToken_anUnknownPathIsNotFound() {
        ServiceCalls.given().when().get("/no/such/path").then().statusCode(404);
    }

    /** UC-006 BR-001: the probes are served outside the REST layer and need no token. */
    @Test
    void healthProbesNeedNoToken() {
        given().when().get("/q/health/live").then().statusCode(200);
        given().when().get("/q/health/ready")
                .then().statusCode(200)
                .body(containsString("\"" + ServiceTokenHealthCheck.NAME + "\""));
        assertEquals(HealthCheckResponse.Status.UP, healthCheck.call().getStatus());
        assertTrue(serviceToken.isConfigured());
    }

    /**
     * UC-006 BR-005: authentication comes first. While the survey is unbuilt, a caller without
     * the token gets the 401 and not the UC-005 instruction; a caller with it gets the 503.
     */
    @Test
    void theTokenIsCheckedBeforeTheSurvey() {
        setReportSchema(null);
        try {
            Response anonymous = given().contentType(ContentType.JSON).body("{\"id\":1}").when().post("/proband/report");
            assertEquals(401, anonymous.getStatusCode());
            assertEquals("", anonymous.getBody().asString());

            Response authenticated = ServiceCalls.given().contentType(ContentType.JSON).body("{\"id\":1}")
                    .when().post("/proband/report");
            assertEquals(503, authenticated.getStatusCode());
            assertTrue(authenticated.getBody().asString().contains("has not been built"));
        } finally {
            setReportSchema("report_family_history_survey");
        }
    }

    private void setReportSchema(String schema) {
        io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() ->
                em.createNativeQuery("UPDATE survey.surveys SET report_schema = ?1 WHERE survey_key = ?2")
                        .setParameter(1, schema)
                        .setParameter(2, java.util.UUID.fromString("5e91c606-59a1-450a-a8d7-2f1530ff472b"))
                        .executeUpdate());
    }

    /**
     * UC-006 BR-004: what an operator's log holds after a refused and an accepted request -
     * the HTTP access log included - carries neither the token nor a presented header.
     */
    @Test
    void theLogNeverCarriesTheToken() throws Exception {
        try (LogCapture log = LogCapture.startAll()) {
            given().header("Authorization", "Bearer presented-but-wrong-marker").contentType(ContentType.JSON)
                    .body("{\"id\":1}").when().post("/proband/report").then().statusCode(401);
            ServiceCalls.given().contentType(ContentType.JSON).body("{\"id\":999999}")
                    .when().post("/proband/report").then().statusCode(200);
            callOwnReport();
            Thread.sleep(200); // the access log record of the last request is written after its response

            assertTrue(log.contains("/proband/report"), "the access log is part of what was captured: " + log.text());
            assertFalse(log.contains(ServiceCalls.TOKEN), log.text());
            assertFalse(log.contains("presented-but-wrong-marker"), log.text());
        }
    }

    /**
     * UC-006 BR-004: FHHS calls its own proband report the way a delivery does (UC-004 BR-004),
     * token attached. The client span of that call and the server span of the request it
     * produced are both recorded, and nothing in either holds the token.
     */
    @Test
    void noSpanOfAServiceCallCarriesTheToken() throws Exception {
        recordedSpans.clear();

        assertNotEquals("Error: Proband", callOwnReport(), "the call must have been accepted");

        List<SpanData> spans = awaitSpans();
        assertTrue(spans.stream().anyMatch(s -> s.getKind() == SpanKind.SERVER), "a server span was recorded: " + names(spans));
        assertTrue(spans.stream().anyMatch(s -> s.getKind() == SpanKind.CLIENT), "a client span was recorded: " + names(spans));
        for (SpanData span : spans) {
            String everything = span.getName() + " " + span.getAttributes() + " " + span.getEvents() + " "
                    + span.getStatus() + " " + span.getResource().getAttributes();
            assertFalse(everything.contains(ServiceCalls.TOKEN), span.getName());
            assertFalse(everything.toLowerCase().contains("bearer"), span.getName() + ": " + everything);
            assertFalse(everything.toLowerCase().contains("authorization"), span.getName() + ": " + everything);
        }
    }

    /** Invokes {@code PDFService.callReport} against this application's own proband report; returns the title. */
    private String callOwnReport() throws Exception {
        String origin = "http://localhost:" + RestAssured.port;
        ReportDefinition report = new ReportDefinition();
        report.name = "Proband";
        report.url = origin + "/proband/report";
        PDFService pdfService = new PDFService();
        java.lang.reflect.Field field = PDFService.class.getDeclaredField("serviceToken");
        field.setAccessible(true);
        field.set(pdfService, ServiceCalls.serviceToken(origin));
        Method callReport = PDFService.class.getDeclaredMethod("callReport", ReportDefinition.class, long.class);
        callReport.setAccessible(true);
        return ((com.elicitsoftware.response.ReportResponse) callReport.invoke(pdfService, report, 999999L)).title;
    }

    private List<SpanData> awaitSpans() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        List<SpanData> spans = recordedSpans.spans();
        while (System.currentTimeMillis() < deadline
                && !(spans.stream().anyMatch(s -> s.getKind() == SpanKind.SERVER)
                && spans.stream().anyMatch(s -> s.getKind() == SpanKind.CLIENT))) {
            Thread.sleep(50);
            spans = recordedSpans.spans();
        }
        return spans;
    }

    private static List<String> names(List<SpanData> spans) {
        return spans.stream().map(s -> s.getKind() + " " + s.getName()).toList();
    }
}
