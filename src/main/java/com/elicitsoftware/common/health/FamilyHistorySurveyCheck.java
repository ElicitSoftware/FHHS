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

import com.elicitsoftware.model.Survey;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.UUID;

/**
 * Answers whether the Family History Survey this application serves is in the database (UC-005).
 * <p>
 * FHHS is specific to one survey: it switches on that survey's step names and reads reporting
 * columns generated from its question set, so no other survey will do. The survey is no longer
 * seeded by a migration; a deployment imports it through Admin. Until that has happened this
 * application starts, says so once in the log (step 2), reports not-ready
 * ({@link FamilyHistorySurveyHealthCheck}) and refuses report requests
 * ({@link FamilyHistorySurveyRequiredFilter}). The survey is recognised by its key (BR-001), not
 * by "any survey exists", because a site running a different survey would otherwise pass the
 * check and fail later on a missing step name.
 * <p>
 * A positive answer is remembered: once imported, the survey stays. A negative answer is
 * re-established on every call so the import is noticed without a restart (BR-002).
 */
@ApplicationScoped
public class FamilyHistorySurveyCheck {

    /** The key of the survey this application serves; {@code family.history.survey.key}. */
    @ConfigProperty(name = "family.history.survey.key")
    String surveyKey;

    private volatile boolean installed;

    public FamilyHistorySurveyCheck() {
        // CDI managed bean
    }

    /** Whether a survey with the configured key exists; re-read until it does. */
    public boolean isSurveyInstalled() {
        if (installed) {
            return true;
        }
        installed = Survey.count("surveyKey = ?1", UUID.fromString(surveyKey)) > 0;
        return installed;
    }

    /** The one instruction an operator needs, in the log, on the readiness probe and on a refusal. */
    public String missingMessage() {
        return "The Family History Survey (key " + surveyKey + ") is not in this database. Import "
                + "FHHS/family-history-survey.elicit through Admin > Apply Survey Definition; this application "
                + "is not ready and refuses report requests until then.";
    }

    /**
     * Logs the instruction once at startup when the survey is absent. Runs after the schema
     * migrator (which observes the same event at priority 1), so the tables exist.
     */
    void onStart(@Observes StartupEvent event) {
        if (!isSurveyInstalled()) {
            // WARN, not INFO: containers run at WARN, and this is the one condition an operator must act on.
            Log.warn(missingMessage());
        }
    }
}
