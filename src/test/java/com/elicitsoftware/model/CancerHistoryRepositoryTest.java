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
 * {@code surveyreport.fact_sections_view} - see {@code research/Kimball_type2.md} section 1.
 * The query itself can't be run without a live Postgres instance, so these tests mock the
 * {@link EntityManager}/{@link Query} layer to lock down two things a refactor must not break:
 * <ol>
 *   <li>the respondent_id parameter is bound as the query's only parameter</li>
 *   <li>row-to-{@link FamilyHistoryRecord} mapping tolerates the JDBC type variance the class
 *       javadoc calls out (Long vs Integer, BigDecimal vs String, nulls)</li>
 * </ol>
 */
class CancerHistoryRepositoryTest {

    private CancerHistoryRepository newRepository(EntityManager em) {
        CancerHistoryRepository repo = new CancerHistoryRepository();
        repo.entityManager = em;
        return repo;
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
        // Regression guard for the exact risk research/Kimball_type2.md documents as resolved:
        // FACT_FHHS_VIEW was dropped in V0.0.7 and must never be reintroduced.
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
