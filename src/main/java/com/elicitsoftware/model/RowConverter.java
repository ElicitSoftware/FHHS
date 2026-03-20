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
import java.util.Objects;

/**
 * Utility class for converting FamilyHistoryRecord data to Row objects.
 * <p>
 * Encapsulates the logic for extracting cancer diagnosis information from
 * FamilyHistoryRecord and converting it to Row objects suitable for
 * display in reports (both HTML and PDF).
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class RowConverter {

    /**
     * String constant for boolean true comparison.
     * <p>
     * Used for matching cancer diagnosis fields in FamilyHistoryRecord.
     */
    private static final String STRING_TRUE = "true";

    /**
     * Default constructor for RowConverter.
     * <p>
     * Required for Javadoc compliance.
     */
    public RowConverter() {
        // Default constructor
    }

    /**
     * Converts a FamilyHistoryRecord to a list of Row objects representing cancer diagnoses.
     * <p>
     * Iterates through all cancer-related fields in the record and creates Row objects
     * for each cancer type where the diagnosis is true.
     * </p>
     *
     * @param record the FamilyHistoryRecord containing cancer information
     * @return list of Row objects representing cancer diagnoses
     */
    public static List<Row> toRows(FamilyHistoryRecord record) {
        List<Row> rows = new ArrayList<>();

        if (record == null) {
            return rows;
        }

        String title = record.step;

        if (STRING_TRUE.equalsIgnoreCase(record.bladderCancer)) {
            rows.add(new Row(title, "Bladder Cancer", record.bladderCancerAge));
            rows.add(new Row(title, "Multiple Bladder Cancers", record.multipleBladdercancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.breastCancer)) {
            rows.add(new Row(title, "Breast Cancer", record.breastCancerAge));
            rows.add(new Row(title, "Triple Negative Breast Cancer", record.tripleNegativeBreastCancer));
            rows.add(new Row(title, "Multiple Breast Cancers", record.multipleBreastcancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.colonOrRectalCancer)) {
            rows.add(new Row(title, "Colon or Rectal Cancer", record.colonOrRectalCancerAge));
            rows.add(new Row(title, "Multiple Colon or Rectal Cancers", record.multipleColonOrRectalCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.endometrialOrUterineCancer)) {
            rows.add(new Row(title, "Endometrial or Uterine Cancer", record.endometrialOrUterineCancerAge));
            rows.add(new Row(title, "Multiple Endometrial or Uterine Cancers", record.multipleEndometrialOrUterineCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.kidneyRenalCellCancer)) {
            rows.add(new Row(title, "Kidney Renal Cell Cancer", record.kidneyRenalCellCancerAge));
            rows.add(new Row(title, "Multiple Kidney Renal Cell Cancers", record.multipleKidneyRenalCellCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.leukemia)) {
            rows.add(new Row(title, "Leukemia", record.leukemiaAge));
            rows.add(new Row(title, "Multiple Leukemias", record.multipleLeukemias));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.lungCancer)) {
            rows.add(new Row(title, "Lung Cancer", record.lungCancerAge));
            rows.add(new Row(title, "Multiple Lung Cancers", record.multipleLungCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.lymphoma)) {
            rows.add(new Row(title, "Lymphoma", record.lymphomaAge));
            rows.add(new Row(title, "Multiple Lymphomas", record.multipleLymphomas));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.melanomaSkinCancer)) {
            rows.add(new Row(title, "Melanoma Skin Cancer", record.melanomaSkinCancerAge));
            rows.add(new Row(title, "Multiple Melanoma Skin Cancers", record.multipleMelanomaSkinCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.nonmelanomaSkinCancer)) {
            rows.add(new Row(title, "Non-Melanoma Skin Cancer", record.nonmelanomaSkinCancerAge));
            rows.add(new Row(title, "Multiple Non-Melanoma Skin Cancers", record.multipleNonmelanomaSkinCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.oralCavityOrThroatCancer)) {
            rows.add(new Row(title, "Oral Cavity or Throat Cancer", record.oralCavityOrThroatCancerAge));
            rows.add(new Row(title, "Multiple Oral Cavity or Throat Cancers", record.multipleOralCavityOrThroatCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.otherCancer)) {
            String cancerName = Objects.requireNonNullElse(record.otherCancerName, "Other Cancer");
            rows.add(new Row(title, cancerName, record.otherAge));
            rows.add(new Row(title, "Multiple " + cancerName + "s", record.multipleOtherCancers));
        }

        /**
         * Adds ovarian cancer rows if diagnosis is true.
         * <p>
         * Includes age at diagnosis and multiple occurrence indicator.
         */
        if (STRING_TRUE.equalsIgnoreCase(record.ovarianCancer)) {
            rows.add(new Row(title, "Ovarian Cancer", record.ovarianCancerAge));
            rows.add(new Row(title, "Multiple Ovarian Cancers", record.multipleOvarianCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.pancreaticCancer)) {
            rows.add(new Row(title, "Pancreatic Cancer", record.pancreaticCancerAge));
            rows.add(new Row(title, "Multiple Pancreatic Cancers", record.multiplePancreaticCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.prostateCancer)) {
            rows.add(new Row(title, "Prostate Cancer", record.prostateCancerAge));
            rows.add(new Row(title, "Multiple Prostate Cancers", record.multipleProstateCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.stomachCancer)) {
            rows.add(new Row(title, "Stomach Cancer", record.stomachCancerAge));
            rows.add(new Row(title, "Multiple Stomach Cancers", record.multipleStomachCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.testicularCancer)) {
            rows.add(new Row(title, "Testicular Cancer", record.testicularCancerAge));
            rows.add(new Row(title, "Multiple Testicular Cancers", record.multipleTesticularCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.thyroidCancer)) {
            rows.add(new Row(title, "Thyroid Cancer", record.thyroidCancerAge));
            rows.add(new Row(title, "Multiple Thyroid Cancers", record.multipleThyroidCancers));
        }

        if (STRING_TRUE.equalsIgnoreCase(record.unknownCancer)) {
            rows.add(new Row(title, "Unknown Cancer", record.unknownCancerAge));
        }

        // Add Ashkenazi ancestry if true
        if (STRING_TRUE.equalsIgnoreCase(record.ashkenazi)) {
            rows.add(new Row(title, "Ashkenazi Ancestry", record.ashkenazi));
        }

        return rows;
    }

}
