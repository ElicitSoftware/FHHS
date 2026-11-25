package com.elicitsoftware.request;

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

import jakarta.validation.constraints.Positive;

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
     * Default constructor.
     */
    public ReportRequest() {
        // Default constructor
    }

    /**
     * The unique identifier of the respondent/participant for whom the report is requested.
     * This ID is used to query the database for relevant family history health survey data.
     * A request whose ID is missing or not positive is answered 400 (NFR-010).
     */
    @Positive
    public long id;
    
    /**
     * The post survey action ID to track the execution status.
     * This ID is used to update the RespondentPSA record with success or failure information.
     */
    public long psaId;
}
