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

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Repository for cancer history queries optimized for minimal data transfer.
 * <p>
 * This repository provides native SQL queries that fetch only the cancer-related
 * columns needed for processing, avoiding the expensive joins and full column
 * selection of the fact_fhhs_view. This significantly improves query performance.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@ApplicationScoped
public class CancerHistoryRepository {

    /**
     * Injected JPA EntityManager for database access.
     * <p>
     * Used for executing native SQL queries and managing persistence context.
     */
    @Inject
    EntityManager entityManager;

    /**
     * Default constructor for CancerHistoryRepository.
     * <p>
     * Required for CDI and Javadoc compliance.
     */
    public CancerHistoryRepository() {
        // Default constructor
    }

    /**
     * Safely converts a database value to Integer.
     * <p>
     * PostgreSQL JDBC drivers return INTEGER columns as Long, not Integer.
     * This helper method handles the conversion and null values.
     * </p>
     *
     * @param value the value from the query result (typically Long or null)
     * @return Integer value or null if input is null
     */
    private Integer castToInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Safely converts a database value to String.
     * <p>
     * PostgreSQL JDBC drivers may return various types for text fields:
     * - String values directly
     * - BigDecimal for numeric/boolean values
     * - Other numeric types
     * This helper method handles all conversions and null values.
     * </p>
     *
     * @param value the value from the query result
     * @return String value or null if input is null
     */
    private String castToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }


    /**
     * Fetches complete family history data for all family members of a respondent.
     * <p>
     * This method queries fact_sections_view DIRECTLY instead of the expensive fact_fhhs_view.
     * The fact_fhhs_view contains a 3-way UNION that's computed on every query. By querying
     * fact_sections_view directly, we get all the demographic and cancer data without the
     * union overhead, and leverage database indexes for performance.
     * </p>
     *
     * @param respondentId the respondent identifier
     * @return list of FamilyHistoryRecord objects with complete family data
     */
    public List<FamilyHistoryRecord> findFamilyHistoryByRespondentId(long respondentId) {
        String sql = """
                SELECT 
                    f.step,
                    f.step_instance,
                    f.relationship,
                    f.age,
                    f.gender,
                    f.vital_status,
                    f.shared_parent,
                    f.ashkenazi,
                    f.bladder_cancer,
                    f.bladder_cancer_age,
                    f.breast_cancer,
                    f.breast_cancer_age,
                    f.triple_negative_breast_cancer,
                    f.colon_or_rectal_cancer,
                    f.colon_or_rectal_cancer_age,
                    f.endometrial_or_uterine_cancer,
                    f.endometrial_or_uterine_cancer_age,
                    f.kidney_renal_cell_cancer,
                    f.kidney_renal_cell_cancer_age,
                    f.leukemia,
                    f.leukemia_age,
                    f.lung_cancer,
                    f.lung_cancer_age,
                    f.lymphoma,
                    f.lymphoma_age,
                    f.melanoma_skin_cancer,
                    f.melanoma_skin_cancer_age,
                    f.nonmelanoma_skin_cancer,
                    f.nonmelanoma_skin_cancer_age,
                    f.oral_cavity_or_throat_cancer,
                    f.oral_cavity_or_throat_cancer_age,
                    f.other_cancer,
                    f.other_age,
                    f.other_cancer_name,
                    f.ovarian_cancer,
                    f.ovarian_cancer_age,
                    f.pancreatic_cancer,
                    f.pancreatic_cancer_age,
                    f.prostate_cancer,
                    f.prostate_cancer_age,
                    f.stomach_cancer,
                    f.stomach_cancer_age,
                    f.testicular_cancer,
                    f.testicular_cancer_age,
                    f.thyroid_cancer,
                    f.thyroid_cancer_age,
                    f.unknown_cancer,
                    f.unknown_cancer_age,
                    f.multiple_bladder_cancers,
                    f.multiple_breast_cancers,
                    f.multiple_colon_or_rectal_cancers,
                    f.multiple_endometrial_or_uterine_cancers,
                    f.multiple_kidney_renal_cell_cancers,
                    f.multiple_leukemias,
                    f.multiple_lung_cancers,
                    f.multiple_lymphomas,
                    f.multiple_melanoma_skin_cancers,
                    f.multiple_nonmelanoma_skin_cancers,
                    f.multiple_oral_cavity_or_throat_cancers,
                    f.multiple_other_cancers,
                    f.multiple_ovarian_cancers,
                    f.multiple_pancreatic_cancers,
                    f.multiple_prostate_cancers,
                    f.multiple_stomach_cancers,
                    f.multiple_testicular_cancers,
                    f.multiple_thyroid_cancers
                FROM surveyreport.fact_sections_view f
                WHERE f.respondent_id = ?1
                ORDER BY f.relationship, f.step
                """;

        var query = entityManager.createNativeQuery(sql);
        query.setParameter(1, respondentId);

        return mapFamilyHistoryResults(query.getResultList());
    }

    /**
     * Maps raw query results to FamilyHistoryRecord objects.
     *
     * @param results the raw query results with demographic and cancer data
     * @return list of FamilyHistoryRecord objects
     */
    private List<FamilyHistoryRecord> mapFamilyHistoryResults(List<?> results) {
        List<FamilyHistoryRecord> records = new ArrayList<>();

        for (Object result : results) {
            if (!(result instanceof Object[])) {
                continue;
            }
            Object[] row = (Object[]) result;
            int index = 0;

            FamilyHistoryRecord record = new FamilyHistoryRecord(
                    castToString(row[index++]),           // step
                    castToString(row[index++]),           // step_instance
                    castToString(row[index++]),           // relationship
                    castToInteger(row[index++]),          // age
                    castToString(row[index++]),           // gender
                    castToString(row[index++]),           // vital_status
                    castToString(row[index++]),           // shared_parent
                    castToString(row[index++]),           // ashkenazi
                    castToString(row[index++]),           // bladder_cancer
                    castToInteger(row[index++]),          // bladder_cancer_age
                    castToString(row[index++]),           // breast_cancer
                    castToInteger(row[index++]),          // breast_cancer_age
                    castToString(row[index++]),           // triple_negative_breast_cancer
                    castToString(row[index++]),           // colon_or_rectal_cancer
                    castToInteger(row[index++]),          // colon_or_rectal_cancer_age
                    castToString(row[index++]),           // endometrial_or_uterine_cancer
                    castToInteger(row[index++]),          // endometrial_or_uterine_cancer_age
                    castToString(row[index++]),           // kidney_renal_cell_cancer
                    castToInteger(row[index++]),          // kidney_renal_cell_cancer_age
                    castToString(row[index++]),           // leukemia
                    castToInteger(row[index++]),          // leukemia_age
                    castToString(row[index++]),           // lung_cancer
                    castToInteger(row[index++]),          // lung_cancer_age
                    castToString(row[index++]),           // lymphoma
                    castToInteger(row[index++]),          // lymphoma_age
                    castToString(row[index++]),           // melanoma_skin_cancer
                    castToInteger(row[index++]),          // melanoma_skin_cancer_age
                    castToString(row[index++]),           // nonmelanoma_skin_cancer
                    castToInteger(row[index++]),          // nonmelanoma_skin_cancer_age
                    castToString(row[index++]),           // oral_cavity_or_throat_cancer
                    castToInteger(row[index++]),          // oral_cavity_or_throat_cancer_age
                    castToString(row[index++]),           // other_cancer
                    castToInteger(row[index++]),          // other_age
                    castToString(row[index++]),           // other_cancer_name
                    castToString(row[index++]),           // ovarian_cancer
                    castToInteger(row[index++]),          // ovarian_cancer_age
                    castToString(row[index++]),           // pancreatic_cancer
                    castToInteger(row[index++]),          // pancreatic_cancer_age
                    castToString(row[index++]),           // prostate_cancer
                    castToInteger(row[index++]),          // prostate_cancer_age
                    castToString(row[index++]),           // stomach_cancer
                    castToInteger(row[index++]),          // stomach_cancer_age
                    castToString(row[index++]),           // testicular_cancer
                    castToInteger(row[index++]),          // testicular_cancer_age
                    castToString(row[index++]),           // thyroid_cancer
                    castToInteger(row[index++]),          // thyroid_cancer_age
                    castToString(row[index++]),           // unknown_cancer
                    castToInteger(row[index++]),          // unknown_cancer_age
                    castToString(row[index++]),           // multiple_bladder_cancers
                    castToString(row[index++]),           // multiple_breast_cancers
                    castToString(row[index++]),           // multiple_colon_or_rectal_cancers
                    castToString(row[index++]),           // multiple_endometrial_or_uterine_cancers
                    castToString(row[index++]),           // multiple_kidney_renal_cell_cancers
                    castToString(row[index++]),           // multiple_leukemias
                    castToString(row[index++]),           // multiple_lung_cancers
                    castToString(row[index++]),           // multiple_lymphomas
                    castToString(row[index++]),           // multiple_melanoma_skin_cancers
                    castToString(row[index++]),           // multiple_nonmelanoma_skin_cancers
                    castToString(row[index++]),           // multiple_oral_cavity_or_throat_cancers
                    castToString(row[index++]),           // multiple_other_cancers
                    castToString(row[index++]),           // multiple_ovarian_cancers
                    castToString(row[index++]),           // multiple_pancreatic_cancers
                    castToString(row[index++]),           // multiple_prostate_cancers
                    castToString(row[index++]),           // multiple_stomach_cancers
                    castToString(row[index++]),           // multiple_testicular_cancers
                    castToString(row[index++])            // multiple_thyroid_cancers
            );
            records.add(record);
        }

        return records;
    }
}

