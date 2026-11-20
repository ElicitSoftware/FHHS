package com.elicitsoftware.familyhistory;

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

import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UC-005 BR-004: the post-survey action FHHS records against is found by its key, because its
 * id is whatever the import minted; an explicit id still wins, and an unknown key yields 0 so
 * no execution record is written (UC-004).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class PostSurveyActionResolutionTest {

    @Inject
    FamilyHistoryReportService service;

    /** The fixture's action carries the configured key and id 1. */
    @Test
    void actionIsFoundByItsKey() {
        assertEquals(1, service.psaId());
    }

    /** An explicit id is used as configured, without a lookup. */
    @Test
    void configuredIdOverridesTheKey() {
        FamilyHistoryReportService pinned = new FamilyHistoryReportService();
        pinned.psaKey = "00000000-0000-0000-0000-000000000000";
        pinned.configuredPsaId = Optional.of(42);

        assertEquals(42, pinned.psaId());
    }

    /** A key nothing carries resolves to 0, and is asked again next time rather than remembered. */
    @Test
    void unknownKeyResolvesToZero() {
        FamilyHistoryReportService unknown = new FamilyHistoryReportService();
        unknown.psaKey = "00000000-0000-0000-0000-000000000000";
        unknown.configuredPsaId = Optional.empty();

        assertEquals(0, unknown.psaId());
        assertEquals(0, unknown.psaId());
    }
}
