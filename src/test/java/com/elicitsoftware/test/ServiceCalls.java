package com.elicitsoftware.test;

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

import com.elicitsoftware.common.security.RespondentScope;
import com.elicitsoftware.common.security.ServiceToken;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import java.util.Optional;

/**
 * UC-006 in the tests: the service token the {@code %test} profile configures, a REST-assured
 * request that presents it the way the Survey Platform does, and plain-built {@link ServiceToken}s
 * for the tests that run without CDI.
 */
public final class ServiceCalls {

    /** The value of {@code %test.elicit.service.token} in {@code src/test/resources/application.properties}. */
    public static final String TOKEN = "fhhs-test-service-token-0123456789-abcdefghij";

    private ServiceCalls() {
    }

    /** A request carrying {@code Authorization: Bearer <test token>}. */
    public static RequestSpecification given() {
        return RestAssured.given().header("Authorization", "Bearer " + TOKEN);
    }

    /** A configured token whose only trusted origin is the test profile's {@code pedigree.url}. */
    public static ServiceToken serviceToken() {
        return serviceToken("");
    }

    /** A configured token that also trusts the given comma-separated origins. */
    public static ServiceToken serviceToken(String trustedOrigins) {
        return new ServiceToken(Optional.of(TOKEN), Optional.of(trustedOrigins), Optional.of("http://localhost:8083/svg"));
    }

    /** What a deployment without {@code ELICIT_SERVICE_TOKEN} has. */
    public static ServiceToken serviceTokenNotConfigured() {
        return new ServiceToken(Optional.empty(), Optional.empty(), Optional.of("http://localhost:8083/svg"));
    }

    /**
     * A scope that lets every respondent through, for a test that builds a service by hand and
     * is about what the service does once the respondent is accepted (RespondentScopeTest is
     * the one about the scope itself).
     */
    public static RespondentScope anyRespondent() {
        return new RespondentScope() {
            @Override
            public boolean contains(long respondentId) {
                return true;
            }

            @Override
            public void require(String endpoint, long respondentId) {
                // accepted
            }
        };
    }
}
