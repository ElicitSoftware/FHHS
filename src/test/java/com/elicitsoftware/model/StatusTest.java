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

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Status}'s date-formatting helpers, which feed directly into
 * {@code FamilyHistoryReportService#generateXmlMetadata} placeholder substitution.
 */
class StatusTest {

    @Test
    void getCreated_formatsCreatedDtAsMmDdYyyy() {
        Status status = new Status();
        Date date = new Date(0); // 1970-01-01 in UTC, exact day depends on local TZ but format is fixed
        status.setCreatedDt(date);

        String expected = new SimpleDateFormat("MM/dd/yyyy").format(date);
        assertEquals(expected, status.getCreated());
    }

    @Test
    void getFinalized_formatsFinalizedDtAsMmDdYyyy() {
        Status status = new Status();
        Date date = new Date(1_700_000_000_000L);
        status.setFinalizedDt(date);

        String expected = new SimpleDateFormat("MM/dd/yyyy").format(date);
        assertEquals(expected, status.getFinalized());
    }

    @Test
    void gettersAndSetters_roundTripCorrectly() {
        Status status = new Status();
        status.setId(1L);
        status.setRespondentId(2L);
        status.setXid("XID-123");
        status.setSurveyId(3L);
        status.setFirstName("Jane");
        status.setLastName("Doe");
        status.setEmail("jane@example.com");
        status.setStatus("COMPLETED");
        status.setDepartmentId(9L);
        status.setDepartmentName("Oncology");

        assertEquals(1L, status.getId());
        assertEquals(2L, status.getRespondentId());
        assertEquals("XID-123", status.getXid());
        assertEquals(3L, status.getSurveyId());
        assertEquals("Jane", status.getFirstName());
        assertEquals("Doe", status.getLastName());
        assertEquals("jane@example.com", status.getEmail());
        assertEquals("COMPLETED", status.getStatus());
        assertEquals(9L, status.getDepartmentId());
        assertEquals("Oncology", status.getDepartmentName());
    }
}
