package com.elicitsoftware.model;

/*-
 * ***LICENSE_START***
 * Elicit Survey
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Tracks one post-survey action for one respondent. Maps the "respondent_psa" table in the
 * "survey" schema.
 * <p>
 * Each row links a respondent to a configured post-survey action (for example, uploading a
 * generated report) and records the attempt count, the current status, the last error message,
 * and when the action was created and completed.
 */
@Entity
@Table(name = "respondent_psa", schema = "survey")
public class RespondentPSA extends PanacheEntityBase {

    /**
     * Default constructor for JPA.
     */
    public RespondentPSA() {
        // Default constructor required by JPA
    }

    /**
     * Unique identifier for the respondent PSA record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RESPONDENT_PSA_ID_GENERATOR")
    @SequenceGenerator(name = "RESPONDENT_PSA_ID_GENERATOR", schema = "survey", sequenceName = "post_survey_actions_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, precision = 20)
    public Integer id;

    /**
     * ID of the respondent associated with this action.
     */
    @Column(name = "respondent_id")
    public long respondentId;

    /**
     * Post survey action ID.
     */
    @Column(name = "post_survey_action_id")
    public long psaId;

    /**
     * Number of upload attempts.
     */
    @Column(name = "tries")
    public long tries;

    /**
     * Current status of the action (e.g., SUCCESS, FAILED).
     */
    @Column(name = "status")
    public String status;

    /**
     * Error message if the action failed.
     */
    @Column(name = "error_msg")
    public String error;

    /**
     * Timestamp when the record was created.
     */
    @Column(name = "created_dt")
    @CreationTimestamp
    public OffsetDateTime createdDt;

    /**
     * Timestamp when the file was successfully uploaded.
     */
    @Column(name = "uploaded_dt")
    public OffsetDateTime uploadedDt;

}
