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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness follows the Family History Survey (UC-005 step 3): down, with the import
 * instruction, until a survey with the configured key exists, then up for good. The container
 * therefore goes healthy on its own once the import lands, with no restart.
 */
@Readiness
@ApplicationScoped
public class FamilyHistorySurveyHealthCheck implements HealthCheck {

    static final String NAME = "Family History Survey installed";

    @Inject
    FamilyHistorySurveyCheck surveyCheck;

    public FamilyHistorySurveyHealthCheck() {
        // CDI managed bean
    }

    @Override
    public HealthCheckResponse call() {
        if (surveyCheck.isSurveyInstalled()) {
            return HealthCheckResponse.up(NAME);
        }
        return HealthCheckResponse.named(NAME).down()
                .withData("reason", surveyCheck.missingMessage())
                .build();
    }
}
