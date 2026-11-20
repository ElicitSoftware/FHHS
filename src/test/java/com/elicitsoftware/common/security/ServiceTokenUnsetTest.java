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

import com.elicitsoftware.test.PostgresTestResource;
import com.elicitsoftware.test.ServiceCalls;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * UC-006 A3, BR-002: an application started without a service token. It starts, is not ready
 * and says which variable to set, and answers every request 503, token or not.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestProfile(ServiceTokenUnsetTest.NoToken.class)
class ServiceTokenUnsetTest {

    /** The deployment that forgot the variable: the property resolves to nothing. */
    public static class NoToken implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("elicit.service.token", "");
        }
    }

    @Inject
    ServiceToken serviceToken;

    @Test
    void startsWithoutAToken() {
        assertFalse(serviceToken.isConfigured());
    }

    /** UC-006 A3 step 2: not ready, through a check named service-token that names the variable. */
    @Test
    void readinessIsDownAndNamesTheVariable() {
        given().when().get("/q/health/ready")
                .then().statusCode(503)
                .body("status", equalTo("DOWN"))
                .body("checks.find { it.name == 'service-token' }.status", equalTo("DOWN"))
                .body("checks.find { it.name == 'service-token' }.data.reason", equalTo("ELICIT_SERVICE_TOKEN is not set"));
        given().when().get("/q/health/live").then().statusCode(200);
    }

    /** UC-006 A3 step 3: every request is refused with 503 and the fixed body, never served. */
    @ParameterizedTest
    @ValueSource(strings = {"/proband/report", "/casummary/report", "/pedigree/report", "/familyhistory/generate",
            "/no/such/path"})
    void everyRequestIsAnswered503(String path) {
        given().contentType(ContentType.JSON).body("{\"id\":1}")
                .when().post(path)
                .then().statusCode(503)
                .contentType(containsString("application/json"))
                .body(equalTo("{\"error\":\"service token not configured\"}"));
        ServiceCalls.given().contentType(ContentType.JSON).body("{\"id\":1}")
                .when().post(path)
                .then().statusCode(503)
                .body("error", equalTo("service token not configured"));
    }
}
