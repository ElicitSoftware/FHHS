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
import jakarta.persistence.*;

import java.util.Set;

/**
 * Represents a survey entity in the system. Each survey contains metadata such as name, title,
 * description, and display information, and references related entities such as reports
 * and post-survey actions.
 * <p>
 * This class is mapped to the "surveys" table within the "survey" schema.
 * It is persistent and managed through JPA.
 */
@Entity
@Table(name = "surveys", schema = "survey")
public class Survey extends PanacheEntityBase {

    /**
     * Default constructor for JPA.
     */
    public Survey() {
        // Default constructor required by JPA
    }

    /**
     * Unique identifier for the survey.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SURVEY_ID_GENERATOR")
    @SequenceGenerator(name = "SURVEY_ID_GENERATOR", schema = "survey", sequenceName = "surveys_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false)
    public Integer id;

    /**
     * Display order for sorting surveys in lists.
     */
    @Column(name = "display_order", nullable = false, precision = 3)
    public Integer displayOrder;

    /**
     * Internal name of the survey.
     */
    @Column(name = "name")
    public String name;

    /**
     * Display title of the survey.
     */
    @Column(name = "title")
    public String title;

    /**
     * Detailed description of the survey.
     */
    @Column(name = "description")
    public String description;

    /**
     * Key used for initial display configuration.
     */
    @Column(name = "initial_display_key")
    public String initialDisplayKey;

    /**
     * URL to redirect to after the survey is completed.
     */
    @Column(name = "post_survey_url")
    public String postSurveyURL;

    /**
     * Set of report definitions associated with this survey.
     */
    @OneToMany(mappedBy = "survey", fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    public Set<ReportDefinition> reports;

    /**
     * Set of post-survey actions to execute after survey completion.
     */
    @OneToMany(mappedBy = "survey", fetch = FetchType.EAGER)
    @OrderBy("executionOrder ASC")
    public Set<PostSurveyAction> postSurveyActions;

}
