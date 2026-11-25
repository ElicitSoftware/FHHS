package com.elicitsoftware.common.security;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness: is the platform service token configured (UC-006 A3).
 *
 * <p>Without it FHHS refuses every request, so it is not ready, and the probe says which
 * variable to set. The reason names the variable and never a value.</p>
 */
@Readiness
@ApplicationScoped
public class ServiceTokenHealthCheck implements HealthCheck {

    static final String NAME = "service-token";

    @Inject
    ServiceToken serviceToken;

    public ServiceTokenHealthCheck() {
        // CDI managed bean
    }

    @Override
    public HealthCheckResponse call() {
        if (serviceToken.isConfigured()) {
            return HealthCheckResponse.up(NAME);
        }
        return HealthCheckResponse.named(NAME).down()
                .withData("reason", ServiceToken.NOT_SET)
                .build();
    }
}
