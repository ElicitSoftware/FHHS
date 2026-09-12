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
 * Verifies V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql, which replaces the
 * surrogate-id-based reorder in V0.0.5__UPDATE_CANCER_QUESTONS.sql - see
 * research/Kimball_type2.md section 2. Runs the real Flyway history (via
 * {@link PostgresTestResource}, whose Kimball Type 2 bootstrap columns/triggers on
 * survey.sections/questions/sections_questions make durable ids equal the fixture's
 * surrogate ids), then asserts on the resulting state of survey.sections_questions.
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
                        + "- the migration's close+insert must leave exactly one current row per id");
    }

    @Test
    @Transactional
    void shiftedQuestion_movedFromDisplayOrderEightToNine() {
        // V0.0.1__POPULATE_FHHS_DATA.sql seeds question_id=11 at section_id=14,
        // display_order=8 (the row immediately before the triple-negative question's
        // insert point). V0.0.8 must have shifted it to display_order=9 to make room.
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
