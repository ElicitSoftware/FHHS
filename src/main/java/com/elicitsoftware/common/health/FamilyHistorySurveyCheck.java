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
import java.util.regex.Pattern;

/**
 * Answers whether the Family History Survey this application serves is in the database (UC-005).
 * <p>
 * FHHS is specific to one survey: it switches on that survey's step names and reads reporting
 * columns generated from its question set, so no other survey will do. The survey is no longer
 * seeded by a migration; a deployment imports it through Admin. Until that has happened this
 * application starts, says so once in the log (step 2), reports not-ready
 * ({@link FamilyHistorySurveyHealthCheck}) and refuses report requests
 * ({@link FamilyHistorySurveyRequiredFilter}). The survey is recognized by its key (BR-001), not
 * by "any survey exists", because a site running a different survey would otherwise pass the
 * check and fail later on a missing step name.
 * <p>
 * A positive answer is remembered: once imported, the survey stays. A negative answer is
 * re-established on every call so the import is noticed without a restart (BR-002).
 * <p>
 * The survey's reporting schema is a second condition (A3): Survey creates one schema per
 * survey and names it on {@code survey.surveys.report_schema}, and a site may rename or drop
 * it, so the name is looked up on every call and never kept (BR-006). Until the survey has
 * been built the application is not ready either.
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

    /**
     * The name of the survey's reporting schema, read from {@code survey.surveys.report_schema}
     * now (BR-006), or {@code null} while the survey is absent or has not been built.
     */
    public String reportSchema() {
        // A scalar query, not an entity load: an entity already in this session would be
        // served from the first-level cache and hide a build, rename or drop made meanwhile.
        java.util.List<String> names = Survey.getEntityManager()
                .createQuery("select s.reportSchema from Survey s where s.surveyKey = ?1", String.class)
                .setParameter(1, UUID.fromString(surveyKey))
                .getResultList();
        return names.isEmpty() ? null : names.get(0);
    }

    /** Whether the survey is in the database with its reporting schema named: ready to serve reports. */
    public boolean isReady() {
        return isSurveyInstalled() && reportSchema() != null;
    }

    /**
     * The one instruction an operator needs, in the log, on the readiness probe and on a
     * refusal: import the survey when it is absent, build it when it is imported and unbuilt.
     */
    public String missingMessage() {
        if (isSurveyInstalled()) {
            return "The Family History Survey (key " + surveyKey + ") is in this database but its reporting "
                    + "schema has not been built (survey.surveys.report_schema is null). Admin's Apply Survey "
                    + "Definition builds it; so does POST /api/etl/build?survey=" + surveyKey + " on Survey. This "
                    + "application is not ready and refuses report requests until then.";
        }
        return "The Family History Survey (key " + surveyKey + ") is not in this database. Import "
                + "FHHS/family-history-survey.elicit through Admin > Apply Survey Definition; this application "
                + "is not ready and refuses report requests until then.";
    }

    /** What an unquoted PostgreSQL identifier looks like; a schema name cannot be bound as a parameter. */
    static final Pattern SCHEMA_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]{0,62}$");

    /**
     * The reporting schema to read from, validated for splicing into native SQL (BR-006).
     *
     * @throws IllegalStateException when the survey is absent or unbuilt, or the stored name is
     *                               not an identifier -- a state the filter normally refuses first
     */
    public String requireReportSchema() {
        String schema = reportSchema();
        if (schema == null) {
            throw new IllegalStateException(missingMessage());
        }
        if (!SCHEMA_PATTERN.matcher(schema).matches()) {
            throw new IllegalStateException("survey.surveys.report_schema is not a schema name: " + schema);
        }
        return schema;
    }

    /**
     * Logs the instruction once at startup when the survey is absent. Runs after the schema
     * migrator (which observes the same event at priority 1), so the tables exist.
     */
    void onStart(@Observes StartupEvent event) {
        if (!isReady()) {
            // WARN, not INFO: containers run at WARN, and this is the one condition an operator must act on.
            Log.warn(missingMessage());
        }
    }
}
