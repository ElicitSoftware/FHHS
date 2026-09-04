package com.elicitsoftware.model;

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

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Respondent#getElapsedTime()}, the only non-JPA logic on this entity.
 * Does not require a database - the entity is only newed up in memory.
 */
class RespondentTest {

    @Test
    void getElapsedTime_bothTimestampsPresent_formatsAsHhMmSs() {
        Respondent r = new Respondent();
        r.firstAccessDt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        r.finalizedDt = OffsetDateTime.parse("2026-01-01T11:30:45Z");

        assertEquals("01:30:45", r.getElapsedTime());
    }

    @Test
    void getElapsedTime_missingFinalizedDt_returnsNotCalculated() {
        Respondent r = new Respondent();
        r.firstAccessDt = OffsetDateTime.now();

        assertEquals("Not calculated", r.getElapsedTime());
    }

    @Test
    void getElapsedTime_missingFirstAccessDt_returnsNotCalculated() {
        Respondent r = new Respondent();
        r.finalizedDt = OffsetDateTime.now();

        assertEquals("Not calculated", r.getElapsedTime());
    }

    @Test
    void getElapsedTime_elapsedOverADay_hoursExceedTwentyFour() {
        Respondent r = new Respondent();
        r.firstAccessDt = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        r.finalizedDt = OffsetDateTime.parse("2026-01-02T02:00:00Z");

        assertEquals("26:00:00", r.getElapsedTime());
    }
}
