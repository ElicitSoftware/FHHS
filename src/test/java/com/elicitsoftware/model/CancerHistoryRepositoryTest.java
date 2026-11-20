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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CancerHistoryRepository}, which is the single query in FHHS that reads
 * the survey's {@code fact_sections_view}, filtered by respondent only: it names no
 * {@code step_key}/{@code section_key} literal, so Survey's dimension surrogate ids never
 * reach it.
 * The query itself can't be run without a live Postgres instance, so these tests mock the
 * {@link EntityManager}/{@link Query} layer to lock down three things a refactor must not break:
 * <ol>
 *   <li>the respondent_id parameter is bound as the query's only parameter</li>
 *   <li>the view is qualified with the reporting schema resolved from the survey at call time,
 *       never a hard-coded one (UC-005 BR-006)</li>
 *   <li>row-to-{@link FamilyHistoryRecord} mapping tolerates the JDBC type variance the class
 *       javadoc calls out (Long vs Integer, BigDecimal vs String, nulls)</li>
 * </ol>
 */
class CancerHistoryRepositoryTest {

    /** A check that answers with a fixed schema name and no database behind it. */
    @jakarta.enterprise.inject.Vetoed
    private static final class FixedSchemaCheck extends com.elicitsoftware.common.health.FamilyHistorySurveyCheck {
        private final String schema;

        FixedSchemaCheck(String schema) {
            this.schema = schema;
        }

        @Override
        public String missingMessage() {
            return "survey.surveys.report_schema is null";
        }

        @Override
        public boolean isSurveyInstalled() {
            return true;
        }

        @Override
        public String reportSchema() {
            return schema;
        }
    }

    private CancerHistoryRepository newRepository(EntityManager em) {
        return newRepository(em, "report_family_history_survey");
    }

    private CancerHistoryRepository newRepository(EntityManager em, String schema) {
        CancerHistoryRepository repo = new CancerHistoryRepository();
        repo.entityManager = em;
        repo.surveyCheck = new FixedSchemaCheck(schema);
        return repo;
    }

    /** UC-005 BR-006: the query reads the schema the survey names, whatever a site called it. */
    @Test
    void findFamilyHistoryByRespondentId_qualifiesTheViewWithTheResolvedSchema() {
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(em.createNativeQuery(sqlCaptor.capture())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        newRepository(em, "report_fhh_renamed").findFamilyHistoryByRespondentId(1L);

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("FROM report_fhh_renamed.fact_sections_view f"), sql);
        assertFalse(sql.contains("surveyreport."), "nothing hard-codes the old site-wide schema");
    }

    /** UC-005 A3 / BR-006: an unbuilt survey has no schema to read; the call fails with the instruction. */
    @Test
    void findFamilyHistoryByRespondentId_refusesWhenTheSurveyHasNoSchema() {
        EntityManager em = mock(EntityManager.class);
        CancerHistoryRepository repo = newRepository(em, null);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> repo.findFamilyHistoryByRespondentId(1L));

        assertTrue(e.getMessage().contains("report_schema is null"), e.getMessage());
        verify(em, never()).createNativeQuery(anyString());
    }

    /** UC-005 BR-006: a stored name that is not an identifier is never spliced into SQL. */
    @Test
    void findFamilyHistoryByRespondentId_refusesASchemaNameThatIsNotAnIdentifier() {
        EntityManager em = mock(EntityManager.class);
        CancerHistoryRepository repo = newRepository(em, "report_x; drop schema survey");

        assertThrows(IllegalStateException.class, () -> repo.findFamilyHistoryByRespondentId(1L));
        verify(em, never()).createNativeQuery(anyString());
    }

    /** One raw row matching the SELECT column order in findFamilyHistoryByRespondentId. */
    private Object[] rawRow(Object step, Object stepInstance, Object relationship, Object age,
                             Object gender, Object vitalStatus, Object sharedParent, Object ashkenazi,
                             Object bladderCancer, Object bladderCancerAge) {
        Object[] row = new Object[66]; // matches the 66-column SELECT / FamilyHistoryRecord constructor
        row[0] = step;
        row[1] = stepInstance;
        row[2] = relationship;
        row[3] = age;
        row[4] = gender;
        row[5] = vitalStatus;
        row[6] = sharedParent;
        row[7] = ashkenazi;
        row[8] = bladderCancer;
        row[9] = bladderCancerAge;
        // remaining 54 columns default to null, which castToString/castToInteger both handle
        return row;
    }

    @Test
    void findFamilyHistoryByRespondentId_bindsRespondentIdAsFirstParameter() {
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        CancerHistoryRepository repo = newRepository(em);

        repo.findFamilyHistoryByRespondentId(123L);

        ArgumentCaptor<Object> paramCaptor = ArgumentCaptor.forClass(Object.class);
        verify(query).setParameter(eq(1), paramCaptor.capture());
        assertEquals(123L, paramCaptor.getValue());
    }

    @Test
    void findFamilyHistoryByRespondentId_queriesFactSectionsViewNotTheRemovedFactView() {
        // Regression guard: FACT_FHHS_VIEW, a union that filtered on hardcoded step_key and
        // section_key literals, was dropped in V0.0.7 and must never be reintroduced.
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(em.createNativeQuery(sqlCaptor.capture())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        CancerHistoryRepository repo = newRepository(em);

        repo.findFamilyHistoryByRespondentId(1L);

        String sql = sqlCaptor.getValue().toLowerCase();
        assertTrue(sql.contains("fact_sections_view"));
        assertFalse(sql.contains("fact_fhhs_view"));
        assertFalse(sql.contains("step_key"), "no hardcoded dimension key literals allowed");
        assertFalse(sql.contains("section_key"), "no hardcoded dimension key literals allowed");
    }

    @Test
    void mapping_typicalPostgresRow_mapsAllFieldsCorrectly() {
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        // Postgres JDBC returns INTEGER columns as Long, per the class javadoc.
        when(query.getResultList()).thenReturn(
                Collections.singletonList(rawRow("Sibling", "1", "Sibling", 25L, "Male", "alive", null, null, "true", 40L))
        );
        CancerHistoryRepository repo = newRepository(em);

        List<FamilyHistoryRecord> result = repo.findFamilyHistoryByRespondentId(1L);

        assertEquals(1, result.size());
        FamilyHistoryRecord r = result.get(0);
        assertEquals("Sibling", r.step);
        assertEquals("1", r.stepInstance);
        assertEquals(25, r.age);
        assertEquals("Male", r.gender);
        assertEquals("alive", r.vitalStatus);
        assertEquals("true", r.bladderCancer);
        assertEquals(40, r.bladderCancerAge);
    }

    @Test
    void mapping_bigDecimalNumericColumn_isCoercedToString() {
        // The class javadoc for castToString explicitly calls out BigDecimal for
        // numeric/boolean columns as a driver quirk that must not throw.
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(
                Collections.singletonList(rawRow("Proband", null, null, null, null, new BigDecimal("1"), null, null, null, null))
        );
        CancerHistoryRepository repo = newRepository(em);

        FamilyHistoryRecord r = repo.findFamilyHistoryByRespondentId(1L).get(0);

        assertEquals("1", r.vitalStatus);
    }

    @Test
    void mapping_nullColumns_mapToNullNotException() {
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(
                Collections.singletonList(rawRow(null, null, null, null, null, null, null, null, null, null))
        );
        CancerHistoryRepository repo = newRepository(em);

        FamilyHistoryRecord r = repo.findFamilyHistoryByRespondentId(1L).get(0);

        assertNull(r.step);
        assertNull(r.age);
        assertNull(r.bladderCancerAge);
    }

    @Test
    void mapping_nonObjectArrayResultRow_isSkippedNotThrown() {
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        // Defensive case: JPA should never actually hand back a bare String for a
        // multi-column native query, but mapFamilyHistoryResults() guards against it anyway.
        when(query.getResultList()).thenReturn(Arrays.asList("not-a-row"));
        CancerHistoryRepository repo = newRepository(em);

        List<FamilyHistoryRecord> result = repo.findFamilyHistoryByRespondentId(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findFamilyHistoryByRespondentId_emptyResultSet_returnsEmptyList() {
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        CancerHistoryRepository repo = newRepository(em);

        assertTrue(repo.findFamilyHistoryByRespondentId(999L).isEmpty());
    }
}
