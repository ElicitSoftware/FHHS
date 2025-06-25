package com.elicitsoftware.request;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

/**
 * Request object for generating reports based on a specific respondent/participant.
 * <p>
 * This simple data transfer object contains the necessary information to identify
 * and retrieve data for a specific participant in the family history health survey.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class ReportRequest {

    /**
     * The unique identifier of the respondent/participant for whom the report is requested.
     * This ID is used to query the database for relevant family history health survey data.
     */
    public long id;
}
