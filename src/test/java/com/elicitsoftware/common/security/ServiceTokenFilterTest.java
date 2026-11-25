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

import com.elicitsoftware.test.ServiceCalls;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC-006: the three answers of the service token filter - 503 when no token is configured (A3,
 * BR-002), 401 for a missing, malformed or wrong credential (A1, A2), and nothing at all for the
 * right one. Plain Java against a mocked request, like {@code FamilyHistorySurveyRequiredFilterTest}.
 */
class ServiceTokenFilterTest {

    private static ContainerRequestContext request(String authorization) {
        ContainerRequestContext context = mock(ContainerRequestContext.class);
        when(context.getHeaderString("Authorization")).thenReturn(authorization);
        return context;
    }

    private static Response refusal(ServiceToken token, String authorization) {
        ServiceTokenFilter filter = new ServiceTokenFilter();
        filter.serviceToken = token;
        ContainerRequestContext context = request(authorization);

        filter.filter(context);

        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(context).abortWith(response.capture());
        return response.getValue();
    }

    /** UC-006 A3, BR-002: no token configured refuses everyone, whatever they present. */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Bearer " + ServiceCalls.TOKEN, "Bearer ", "Bearer anything"})
    void answers503WhenNoTokenIsConfigured(String authorization) {
        Response response = refusal(ServiceCalls.serviceTokenNotConfigured(), authorization);

        assertEquals(503, response.getStatus());
        assertEquals("{\"error\":\"service token not configured\"}", response.getEntity());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    /** UC-006 A1, A2: missing, another scheme, empty and wrong are one answer, with no body. */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "Bearer",
            "Bearer ",
            "Bearer wrong",
            "Bearer " + ServiceCalls.TOKEN + "x",
            "Basic " + ServiceCalls.TOKEN,
            ServiceCalls.TOKEN,
            "Token " + ServiceCalls.TOKEN,
            "Bearer" + ServiceCalls.TOKEN,
            "Bearer x " + ServiceCalls.TOKEN})
    void answers401ForAMissingOrWrongCredential(String authorization) {
        Response response = refusal(ServiceCalls.serviceToken(), authorization);

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeaderString("WWW-Authenticate"));
        assertFalse(response.hasEntity(), "missing and wrong are indistinguishable: no body");
    }

    /** UC-006 main scenario: the right token passes. The scheme name is case-insensitive (RFC 9110). */
    @ParameterizedTest
    @ValueSource(strings = {"Bearer " + ServiceCalls.TOKEN, "bearer " + ServiceCalls.TOKEN, "Bearer  " + ServiceCalls.TOKEN})
    void passesTheConfiguredToken(String authorization) {
        ServiceTokenFilter filter = new ServiceTokenFilter();
        filter.serviceToken = ServiceCalls.serviceToken();
        ContainerRequestContext context = request(authorization);

        filter.filter(context);

        verify(context, never()).abortWith(any());
    }

    @Test
    void presentedValueIsWhatFollowsTheScheme() {
        assertEquals("abc", ServiceTokenFilter.presented("Bearer abc"));
        assertEquals("", ServiceTokenFilter.presented("Bearer "));
        assertNull(ServiceTokenFilter.presented(null));
        assertNull(ServiceTokenFilter.presented("Basic abc"));
    }
}
