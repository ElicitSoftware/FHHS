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

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Refuses report requests while the Family History Survey is absent (UC-005 step 4): every
 * report endpoint would otherwise fail part-way through on a missing step or column. The
 * answer is 503 with the same instruction the log and the readiness probe carry, so a caller
 * (Survey's report links, its post-survey action) sees why rather than a stack trace. The
 * liveness-style health endpoint and the debug endpoint stay reachable.
 */
@Provider
public class FamilyHistorySurveyRequiredFilter implements ContainerRequestFilter {

    @Inject
    FamilyHistorySurveyCheck surveyCheck;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (isAlwaysAllowed(requestContext.getUriInfo().getPath()) || surveyCheck.isSurveyInstalled()) {
            return;
        }
        requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.TEXT_PLAIN)
                .entity(surveyCheck.missingMessage())
                .build());
    }

    /** The endpoints that answer without the survey: they exist to say what is wrong. */
    static boolean isAlwaysAllowed(String path) {
        String p = path.startsWith("/") ? path.substring(1) : path;
        return p.equals("familyhistory/health") || p.startsWith("familyhistory/debug/");
    }
}
