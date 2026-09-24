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
 * UC-005 (Refuse Service Until the Survey Is Imported): the survey is recognised by its key,
 * readiness follows it, and the report endpoints refuse without it. The test fixture seeds the
 * Family History Survey (db/test), so presence is the ordinary case here; absence is exercised
 * with a key nothing carries.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class FamilyHistorySurveyCheckTest {

    @Inject
    FamilyHistorySurveyCheck surveyCheck;

    @Inject
    @Readiness
    FamilyHistorySurveyHealthCheck healthCheck;

    /** UC-005 BR-001: the seeded survey carries the configured key, so the check passes. */
    @Test
    void surveyWithTheConfiguredKeyIsInstalled() {
        assertTrue(surveyCheck.isSurveyInstalled());
        assertEquals(HealthCheckResponse.Status.UP, healthCheck.call().getStatus());
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
