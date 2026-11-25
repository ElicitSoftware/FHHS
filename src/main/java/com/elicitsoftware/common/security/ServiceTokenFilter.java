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

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Requires the platform service token on every REST request (UC-006).
 *
 * <p>It runs before a resource is matched, so it covers every path FHHS serves, paths added
 * later and paths that do not exist alike (BR-001); there is no list of exemptions. The health
 * probes under {@code /q/health} are served by Quarkus outside the REST layer and never reach
 * it.</p>
 *
 * <p>Order: this filter is pre-matching and has priority {@link Priorities#AUTHENTICATION}, so
 * it runs before {@code FamilyHistorySurveyRequiredFilter} on both counts. A caller without
 * the token is answered 401 and never sees that filter's instruction (BR-005).</p>
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class ServiceTokenFilter implements ContainerRequestFilter {

    /** UC-006 A3: the whole body of the 503. */
    static final String NOT_CONFIGURED_BODY = "{\"error\":\"" + ServiceToken.NOT_CONFIGURED + "\"}";

    private static final String SCHEME = "Bearer ";

    @Inject
    ServiceToken serviceToken;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!serviceToken.isConfigured()) {
            // BR-002: fail closed. No token here means nobody can be recognized, so nobody is served.
            requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(NOT_CONFIGURED_BODY)
                    .build());
            return;
        }
        if (!serviceToken.matches(presented(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)))) {
            // A1, A2: one answer for a missing, malformed and wrong credential. Nothing is logged:
            // the access log already records the request, and the header must not be (BR-004).
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                    .build());
        }
    }

    /** The value after {@code Bearer}, or {@code null} when the header is absent or of another scheme. */
    static String presented(String header) {
        if (header == null || !header.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
            return null;
        }
        return header.substring(SCHEME.length()).strip();
    }
}
