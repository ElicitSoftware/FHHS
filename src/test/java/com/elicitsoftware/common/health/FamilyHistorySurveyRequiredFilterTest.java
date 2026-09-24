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

import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC-005 step 4 / BR-003: while the survey is absent, report requests are refused with 503 and
 * the instruction; the endpoints that exist to say what is wrong stay reachable.
 */
class FamilyHistorySurveyRequiredFilterTest {

    /** A check with a fixed answer and no database behind it; vetoed so CDI never sees a second bean. */
    @Vetoed
    private static final class FixedCheck extends FamilyHistorySurveyCheck {
        private final boolean installed;

        FixedCheck(boolean installed) {
            this.installed = installed;
            this.surveyKey = "5e91c606-59a1-450a-a8d7-2f1530ff472b";
        }

        @Override
        public boolean isSurveyInstalled() {
            return installed;
        }
    }

    private static ContainerRequestContext request(String path) {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn(path);
        ContainerRequestContext context = mock(ContainerRequestContext.class);
        when(context.getUriInfo()).thenReturn(uriInfo);
        return context;
    }

    private static FamilyHistorySurveyRequiredFilter filter(boolean installed) {
        FamilyHistorySurveyRequiredFilter filter = new FamilyHistorySurveyRequiredFilter();
        filter.surveyCheck = new FixedCheck(installed);
        return filter;
    }

    /** UC-005 step 4: a report request is refused with 503 and the instruction. */
    @Test
    void refusesReportRequestsWhileTheSurveyIsAbsent() {
        ContainerRequestContext context = request("proband/report");

        filter(false).filter(context);

        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(context).abortWith(response.capture());
        assertEquals(503, response.getValue().getStatus());
        assertTrue(String.valueOf(response.getValue().getEntity()).contains("Apply Survey Definition"));
    }

    /** UC-005 BR-003: the health and debug endpoints are never refused. */
    @Test
    void healthAndDebugStayReachable() {
        for (String path : new String[] {"familyhistory/health", "/familyhistory/health", "familyhistory/debug/status/7"}) {
            ContainerRequestContext context = request(path);
            filter(false).filter(context);
            verify(context, never()).abortWith(org.mockito.ArgumentMatchers.any());
        }
        assertTrue(FamilyHistorySurveyRequiredFilter.isAlwaysAllowed("familyhistory/health"));
        assertFalse(FamilyHistorySurveyRequiredFilter.isAlwaysAllowed("familyhistory/generate"));
    }

    /** UC-005: once the survey is present nothing is refused. */
    @Test
    void passesEverythingWhenTheSurveyIsPresent() {
        ContainerRequestContext context = request("familyhistory/generate");

        filter(true).filter(context);

        verify(context, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }
}
