package com.elicitsoftware.common.security;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.model.Respondent;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.UUID;

/**
 * Whether a respondent is one FHHS serves: a respondent of the Family History Survey
 * ({@code family.history.survey.key}) and of no other (UC-006 BR-009, NFR-014).
 *
 * <p>The service token says that a platform module is calling and nothing about whom it asks
 * for (UC-006 BR-008), so every endpoint that takes a respondent ID asks here first, before it
 * reads any report data. An ID that does not exist and an ID of another survey's respondent are
 * answered alike, so the answer does not say which it was.</p>
 */
@ApplicationScoped
public class RespondentScope {

    /** The fixed body of the 404 (NFR-010): no ID, no survey, no cause. */
    public static final String NOT_FOUND_MESSAGE = "No respondent of the Family History Survey has this ID.";

    /** The key of the survey this application serves; {@code family.history.survey.key}. */
    @ConfigProperty(name = "family.history.survey.key")
    String surveyKey;

    public RespondentScope() {
        // CDI managed bean
    }

    /** Whether the ID is that of a respondent of the Family History Survey. */
    public boolean contains(long respondentId) {
        if (respondentId <= 0 || respondentId > Integer.MAX_VALUE) {
            return false;
        }
        // A count, not an entity load: nothing of the respondent is read to decide this.
        return Respondent.count("id = ?1 and survey.surveyKey = ?2",
                (int) respondentId, UUID.fromString(surveyKey)) > 0;
    }

    /**
     * Lets a request for a respondent of the Family History Survey through and records it;
     * refuses any other with 404 (UC-006 A8).
     *
     * @param endpoint     the request path, for the access record
     * @param respondentId the respondent the caller asks about
     * @throws WebApplicationException 404 with {@link #NOT_FOUND_MESSAGE} when the respondent is
     *                                 not one of the survey's
     */
    public void require(String endpoint, long respondentId) {
        if (!contains(respondentId)) {
            // NFR-009: the respondent ID is the one identifier a log record may carry.
            Log.warnf("Refused %s for respondent %d: not a respondent of the Family History Survey", endpoint,
                    respondentId);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(NOT_FOUND_MESSAGE)
                    .build());
        }
        // UC-006 BR-009: one record per accepted request, the endpoint and the respondent ID only.
        Log.infof("Report request %s for respondent %d", endpoint, respondentId);
    }
}
