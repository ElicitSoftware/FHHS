package com.elicitsoftware.common.health;

/*-
 * ***LICENSE_START***
 * Elicit Survey
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-005 (Refuse Service Until the Survey Is Imported): the survey is recognized by its key,
 * readiness follows it and its reporting schema, and the report endpoints refuse without them.
 * The test fixture seeds the Family History Survey (db/test) with its schema named, so presence
 * is the ordinary case here; absence is exercised with a key nothing carries, and the unbuilt
 * state (A3) by clearing report_schema for the duration of one test.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class FamilyHistorySurveyCheckTest {

    @Inject
    FamilyHistorySurveyCheck surveyCheck;

    @Inject
    @Readiness
    FamilyHistorySurveyHealthCheck healthCheck;

    @Inject
    jakarta.persistence.EntityManager em;

    /** UC-005 BR-001: the seeded survey carries the configured key, so the check passes. */
    @Test
    void surveyWithTheConfiguredKeyIsInstalled() {
        assertTrue(surveyCheck.isSurveyInstalled());
        assertTrue(surveyCheck.isReady());
        assertEquals(HealthCheckResponse.Status.UP, healthCheck.call().getStatus());
    }

    /** UC-005 BR-006: the schema name comes from the survey row, read now, and is fit to splice. */
    @Test
    void reportSchemaIsReadFromTheSurveyRow() {
        assertEquals("report_family_history_survey", surveyCheck.reportSchema());
        assertEquals("report_family_history_survey", surveyCheck.requireReportSchema());
    }

    /**
     * UC-005 A3 / BR-002 / BR-006: a survey that is imported but not built is not ready, the
     * instruction says to build it, the probe and the filter follow at once, and so does the
     * build when it lands -- nothing is cached.
     */
    @Test
    void surveyWithoutAReportingSchemaIsNotReadyUntilItIsBuilt() {
        setReportSchema(null);
        try {
            assertTrue(surveyCheck.isSurveyInstalled(), "the survey itself is still there");
            assertFalse(surveyCheck.isReady());
            assertEquals(null, surveyCheck.reportSchema());
            assertTrue(surveyCheck.missingMessage().contains("report_schema is null"), surveyCheck.missingMessage());
            assertTrue(surveyCheck.missingMessage().contains("/api/etl/build?survey=5e91c606-59a1-450a-a8d7-2f1530ff472b"),
                    "the instruction says how to build it: " + surveyCheck.missingMessage());
            HealthCheckResponse down = healthCheck.call();
            assertEquals(HealthCheckResponse.Status.DOWN, down.getStatus());
            assertTrue(String.valueOf(down.getData().orElseThrow().get("reason")).contains("has not been built"));
            given().when().get("/q/health/ready").then().statusCode(503);
            assertEquals(503, given().contentType("application/json").body("{}").when().post("/familyhistory/generate").getStatusCode(),
                    "the filter refuses report requests while the survey is unbuilt");
        } finally {
            setReportSchema("report_family_history_survey");
        }
        assertTrue(surveyCheck.isReady(), "the build is noticed on the next call");
        assertEquals(HealthCheckResponse.Status.UP, healthCheck.call().getStatus());
    }

    private void setReportSchema(String schema) {
        io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() ->
                em.createNativeQuery("UPDATE survey.surveys SET report_schema = ?1 WHERE survey_key = ?2")
                        .setParameter(1, schema)
                        .setParameter(2, java.util.UUID.fromString("5e91c606-59a1-450a-a8d7-2f1530ff472b"))
                        .executeUpdate());
    }

    /** UC-005 BR-001: any survey is not enough; the key must match. */
    @Test
    void surveyWithAnotherKeyDoesNotCount() {
        FamilyHistorySurveyCheck other = new FamilyHistorySurveyCheck();
        other.surveyKey = "00000000-0000-0000-0000-000000000000";

        assertFalse(other.isSurveyInstalled());
        assertTrue(other.missingMessage().contains("00000000-0000-0000-0000-000000000000"),
                "the instruction names the key that is missing");
        assertTrue(other.missingMessage().contains("Apply Survey Definition"),
                "the instruction says how to fix it");
    }

    /** UC-005 step 3: readiness is served through the standard probe the container checks. */
    @Test
    void readinessProbeReportsTheSurvey() {
        given().when().get("/q/health/ready")
                .then().statusCode(200)
                .body("status", equalTo("UP"))
                .body(containsString(FamilyHistorySurveyHealthCheck.NAME));
    }

    /** UC-005 step 4: the report endpoints answer once the survey is present (no 503). */
    @Test
    void reportEndpointsAreNotRefusedWhenTheSurveyIsPresent() {
        int status = given().when().get("/familyhistory/debug/status/999999").getStatusCode();
        assertTrue(status != 503, "the filter must not refuse while the survey is installed, got " + status);
    }
}
