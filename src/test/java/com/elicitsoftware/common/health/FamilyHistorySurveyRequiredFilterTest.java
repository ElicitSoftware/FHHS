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
 * UC-005 step 4 / A3 / BR-003: while the survey is absent, or present but unbuilt, report
 * requests are refused with 503 and the instruction; the endpoints that exist to say what is
 * wrong stay reachable.
 */
class FamilyHistorySurveyRequiredFilterTest {

    /** A check with fixed answers and no database behind it; vetoed so CDI never sees a second bean. */
    @Vetoed
    private static final class FixedCheck extends FamilyHistorySurveyCheck {
        private final boolean installed;
        private final String schema;

        FixedCheck(boolean installed, String schema) {
            this.installed = installed;
            this.schema = schema;
            this.surveyKey = "5e91c606-59a1-450a-a8d7-2f1530ff472b";
        }

        @Override
        public boolean isSurveyInstalled() {
            return installed;
        }

        @Override
        public String reportSchema() {
            return schema;
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
        return filter(installed, installed ? "report_family_history_survey" : null);
    }

    private static FamilyHistorySurveyRequiredFilter filter(boolean installed, String schema) {
        FamilyHistorySurveyRequiredFilter filter = new FamilyHistorySurveyRequiredFilter();
        filter.surveyCheck = new FixedCheck(installed, schema);
        return filter;
    }

    /** UC-005 A3: an imported but unbuilt survey is refused too, with the build instruction. */
    @Test
    void refusesReportRequestsWhileTheSurveyIsUnbuilt() {
        ContainerRequestContext context = request("proband/report");

        filter(true, null).filter(context);

        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(context).abortWith(response.capture());
        assertEquals(503, response.getValue().getStatus());
        assertTrue(String.valueOf(response.getValue().getEntity()).contains("has not been built"));
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
