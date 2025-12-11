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
 * Represents a respondent in a survey system. A respondent is associated with a survey
 * and interacts with it through various actions such as accessing and submitting responses.
 * This entity tracks key events and properties of the respondent's interaction with the survey.
 * <p>
 * The Respondent entity is mapped to the "respondents" table within the "survey" schema
 * and is managed through JPA. It supports named queries for retrieving specific respondent
 * data based on survey and token criteria.
 * <p>
 * Key features of this class include:
 * - Tracking of creation, first access, and finalization timestamps.
 * - Management of an active status to indicate if the respondent is currently participating.
 * - Storage of a unique token to identify individual respondents securely.
 * - Reference to the associated survey.
 * <p>
 * Named Queries:
 * - "Respondent.findBySurveyAndToken": Finds a respondent by the given survey ID and token.
 * - "Respondent.findActiveByToken": Retrieves active respondents associated with a specific token,
 * ordered by survey ID.
 * <p>
 * The entity includes utility methods such as:
 * - `findBySurveyAndToken`: Static method to retrieve a respondent based on survey ID and token.
 * - `getElapsedTime`: Calculates the elapsed time between the first access and finalization
 * timestamps, if available, formatted as HH:mm:ss.
 */
@Entity
@Table(name = "respondents", schema = "survey")
@NamedQueries({
        @NamedQuery(name = "Respondent.findBySurveyAndToken", query = "SELECT R FROM Respondent R where R.survey.id = :survey_id and R.token = :token"),
        @NamedQuery(name = "Respondent.findActiveByToken", query = "SELECT R FROM Respondent R where R.token = :token and R.active = true order by R.survey.id")
})
public class Respondent extends PanacheEntityBase {

    /**
     * Default constructor for JPA.
     */
    public Respondent() {
        // Default constructor required by JPA
    }

    /**
     * Unique identifier for the respondent.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RESPONDENT_ID_GENERATOR")
    @SequenceGenerator(name = "RESPONDENT_ID_GENERATOR", schema = "survey", sequenceName = "respondents_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, precision = 20)
    public Integer id;

    /**
     * Timestamp when the respondent record was created.
     */
    @Column(name = "created_dt")
    @CreationTimestamp
    public OffsetDateTime createdDt;

    /**
     * Timestamp of the first time the respondent accessed the survey.
     */
    @Column(name = "first_access_dt")
    public OffsetDateTime firstAccessDt;

    /**
     * Timestamp when the respondent finalized the survey.
     */
    @Column(name = "finalized_dt")
    public OffsetDateTime finalizedDt;

    /**
     * Indicates whether this respondent record is active.
     */
    public boolean active;

    /**
     * Number of times the respondent has logged in.
     */
    public int logins;

    /**
     * The survey associated with this respondent.
     */
    @ManyToOne
    @JoinColumn(name = "survey_id", nullable = false)
    public Survey survey;

    /**
     * Unique token for accessing the survey.
     */
    public String token;

    /**
     * Finds a respondent by survey ID and token.
     *
     * @param survey_id the survey ID
     * @param token the respondent token
     * @return the matching Respondent or null if not found
     */
    @Transient
    public static Respondent findBySurveyAndToken(Integer survey_id, String token) {
        return Respondent.find("survey.id = :survey_id and token = :token", Parameters.with("survey_id", survey_id).and("token", token)).firstResult();
    }

    /**
     * Calculates the elapsed time between first access and finalization.
     *
     * @return formatted time string (HH:mm:ss) or null if times not available
     */
    @Transient
    public String getElapsedTime() {
        if (firstAccessDt != null && finalizedDt != null) {
            Duration duration = Duration.between(firstAccessDt, finalizedDt);
            long elapsedMilliseconds = duration.toMillis();
            return String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(elapsedMilliseconds),
                    TimeUnit.MILLISECONDS.toMinutes(elapsedMilliseconds)
                            - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS
                            .toHours(elapsedMilliseconds)),
                    TimeUnit.MILLISECONDS.toSeconds(elapsedMilliseconds)
                            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
                            .toMinutes(elapsedMilliseconds)));

        }
        return "Not calculated";
    }
}
