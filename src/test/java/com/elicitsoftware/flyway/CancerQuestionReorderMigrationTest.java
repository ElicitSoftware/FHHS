package com.elicitsoftware.flyway;

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

import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the order of the Cancers section after the greenfield Flyway history has run:
 * V0.0.1__POPULATE_FHHS_DATA.sql seeds the Triple Negative breast cancer question at
 * display_order 8 directly (the reorder V0.0.5 / V0.0.8 applied on the released V2.x track
 * is folded into the seed, and both are no-ops on this track - see
 * research/Kimball_type2.md section 2). Runs the real Flyway history via
 * {@link PostgresTestResource}, then asserts on the resulting state of
 * survey.sections_questions.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class CancerQuestionReorderMigrationTest {

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void tripleNegativeQuestion_isCurrentlyOrderedAtDisplayOrderEight() {
        BigDecimal displayOrder = (BigDecimal) entityManager.createNativeQuery(
                        "SELECT sq.display_order " +
                                "FROM survey.sections_questions sq " +
                                "JOIN survey.questions q ON q.question_id = sq.question_id " +
                                "  AND q.effective_from <= NOW() AND q.effective_to > NOW() " +
                                "JOIN survey.sections s ON s.section_id = sq.section_id " +
                                "  AND s.effective_from <= NOW() AND s.effective_to > NOW() " +
                                "WHERE s.dimension_name = 'Cancers' " +
                                "  AND q.short_text = 'Triple Negative' " +
                                "  AND sq.effective_from <= NOW() AND sq.effective_to > NOW()")
                .getSingleResult();

        assertEquals(new BigDecimal(8), displayOrder);
    }

    @Test
    @Transactional
    @SuppressWarnings("unchecked")
    void cancerSection_hasExactlyOneCurrentRowPerDurableQuestionMapping() {
        List<Object[]> duplicates = entityManager.createNativeQuery(
                        "SELECT sq.sections_question_id, COUNT(*) " +
                                "FROM survey.sections_questions sq " +
                                "JOIN survey.sections s ON s.section_id = sq.section_id " +
                                "  AND s.effective_from <= NOW() AND s.effective_to > NOW() " +
                                "WHERE s.dimension_name = 'Cancers' " +
                                "  AND sq.effective_from <= NOW() AND sq.effective_to > NOW() " +
                                "GROUP BY sq.sections_question_id " +
                                "HAVING COUNT(*) > 1")
                .getResultList();

        assertEquals(0, duplicates.size(),
                "found duplicate 'current' sections_questions rows for the same durable id "
                        + "- the seed must leave exactly one current row per id");
    }

    @Test
    @Transactional
    void shiftedQuestion_movedFromDisplayOrderEightToNine() {
        // Before the reorder was folded into the seed, question_id=11 sat at section_id=14,
        // display_order=8 (the row immediately before the triple-negative question's
        // insert point). V0.0.1 must now seed it at display_order=9 to make room.
        BigDecimal displayOrder = (BigDecimal) entityManager.createNativeQuery(
                        "SELECT sq.display_order " +
                                "FROM survey.sections_questions sq " +
                                "JOIN survey.sections s ON s.section_id = sq.section_id " +
                                "  AND s.effective_from <= NOW() AND s.effective_to > NOW() " +
                                "WHERE s.dimension_name = 'Cancers' " +
                                "  AND sq.effective_from <= NOW() AND sq.effective_to > NOW() " +
                                "  AND sq.question_id = (" +
                                "    SELECT q.question_id FROM survey.questions q WHERE q.id = 11" +
                                "  )")
                .getSingleResult();

        assertEquals(new BigDecimal(9), displayOrder);
    }
}
