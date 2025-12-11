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

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing the consolidated family history health survey view.
 * <p>
 * This class maps to the "fact_fhhs_view" database view and contains comprehensive
 * family member health information including demographics, relationships, and
 * detailed cancer history data. It serves as the primary data source for generating
 * family health reports and pedigree charts.
 * </p>
 * <p>
 * The entity includes fields for various cancer types with both occurrence indicators
 * and age at diagnosis, as well as multiple cancer occurrence flags for complex
 * medical histories.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@Entity
@Table(name = "fact_fhhs_view", schema = "surveyreport")
public class FactFHHSView extends PanacheEntityBase {

    /**
     * Default constructor for JPA.
     */
    public FactFHHSView() {
        // Default constructor required by JPA
    }

    /**
     * Constant representing boolean true in survey responses.
     */
    @Transient
    private static final String TRUE = "true";

    /**
     * Primary key identifier for this fact record.
     */
    @Id
    public long id;

    /**
     * Identifier linking this record to a specific survey respondent.
     */
    public long respondent_id;

    /**
     * Name of the family member.
     */
    public String name;

    /**
     * Step instance number in the survey progression.
     */
    public Integer step_instance;

    /**
     * Age of the family member.
     */
    public Integer age;

    /**
     * Ashkenazi Jewish ancestry indicator.
     */
    public String ashkenazi;

    // Cancer-specific fields with age at diagnosis
    /**
     * Age at bladder cancer diagnosis.
     */
    public Integer bladder_cancer_age;

    /**
     * Bladder cancer occurrence indicator.
     */
    public String bladder_cancer;

    /**
     * Age at breast cancer diagnosis.
     */
    public Integer breast_cancer_age;

    /**
     * Breast cancer occurrence indicator.
     */
    public String breast_cancer;

    /**
     * Age at colorectal cancer diagnosis.
     */
    public Integer colon_or_rectal_cancer_age;

    /**
     * Colorectal cancer occurrence indicator.
     */
    public String colon_or_rectal_cancer;

    /**
     * Age at endometrial/uterine cancer diagnosis.
     */
    public Integer endometrial_or_uterine_cancer_age;

    /**
     * Endometrial/uterine cancer occurrence indicator.
     */
    public String endometrial_or_uterine_cancer;

    /**
     * Gender of the family member.
     */
    public String gender;

    /**
     * Generation level in the family tree.
     */
    public String generation;

    /**
     * Age at kidney/renal cell cancer diagnosis.
     */
    public Integer kidney_renal_cell_cancer_age;

    /**
     * Kidney/renal cell cancer occurrence indicator.
     */
    public String kidney_renal_cell_cancer;

    /**
     * Latino/Hispanic ethnicity indicator.
     */
    public String latinx;

    /**
     * Age at leukemia diagnosis.
     */
    public Integer leukemia_age;

    /**
     * Leukemia occurrence indicator.
     */
    public String leukemia;

    /**
     * Age at lung cancer diagnosis.
     */
    public Integer lung_cancer_age;

    /**
     * Lung cancer occurrence indicator.
     */
    public String lung_cancer;

    /**
     * Age at lymphoma diagnosis.
     */
    public Integer lymphoma_age;

    /**
     * Lymphoma occurrence indicator.
     */
    public String lymphoma;

    /**
     * Age at melanoma skin cancer diagnosis.
     */
    public Integer melanoma_skin_cancer_age;

    /**
     * Melanoma skin cancer occurrence indicator.
     */
    public String melanoma_skin_cancer;

    // Multiple cancer occurrence indicators
    /**
     * Multiple bladder cancer occurrences indicator.
     */
    public String multiple_bladder_cancers;

    /**
     * Multiple breast cancer occurrences indicator.
     */
    public String multiple_breast_cancers;

    /**
     * Multiple colorectal cancer occurrences indicator.
     */
    public String multiple_colon_or_rectal_cancers;

    /**
     * Multiple endometrial/uterine cancer occurrences indicator.
     */
    public String multiple_endometrial_or_uterine_cancers;

    /**
     * Multiple kidney/renal cell cancer occurrences indicator.
     */
    public String multiple_kidney_renal_cell_cancers;

    /**
     * Multiple leukemia occurrences indicator.
     */
    public String multiple_leukemias;

    /**
     * Multiple lung cancer occurrences indicator.
     */
    public String multiple_lung_cancers;

    /**
     * Multiple lymphoma occurrences indicator.
     */
    public String multiple_lymphomas;

    /**
     * Multiple melanoma skin cancer occurrences indicator.
     */
    public String multiple_melanoma_skin_cancers;

    /**
     * Multiple non-melanoma skin cancer occurrences indicator.
     */
    public String multiple_nonmelanoma_skin_cancers;

    /**
     * Multiple oral cavity/throat cancer occurrences indicator.
     */
    public String multiple_oral_cavity_or_throat_cancers;

    /**
     * Multiple other cancer occurrences indicator.
     */
    public String multiple_other_cancers;

    /**
     * Multiple ovarian cancer occurrences indicator.
     */
    public String multiple_ovarian_cancers;

    /**
     * Multiple pancreatic cancer occurrences indicator.
     */
    public String multiple_pancreatic_cancers;

    /**
     * Multiple prostate cancer occurrences indicator.
     */
    public String multiple_prostate_cancers;

    /**
     * Multiple stomach cancer occurrences indicator.
     */
    public String multiple_stomach_cancers;

    /**
     * Multiple testicular cancer occurrences indicator.
     */
    public String multiple_testicular_cancers;

    /**
     * Multiple thyroid cancer occurrences indicator.
     */
    public String multiple_thyroid_cancers;

    /**
     * Age at non-melanoma skin cancer diagnosis.
     */
    public Integer nonmelanoma_skin_cancer_age;

    /**
     * Non-melanoma skin cancer occurrence indicator.
     */
    public String nonmelanoma_skin_cancer;

    /**
     * Age at oral cavity/throat cancer diagnosis.
     */
    public Integer oral_cavity_or_throat_cancer_age;

    /**
     * Oral cavity/throat cancer occurrence indicator.
     */
    public String oral_cavity_or_throat_cancer;

    /**
     * Age at other cancer type diagnosis.
     */
    public Integer other_age;

    /**
     * Other cancer occurrence indicator.
     */
    public String other_cancer;

    /**
     * Specific name/type of other cancer.
     */
    public String other_cancer_name;

    /**
     * Age at ovarian cancer diagnosis.
     */
    public Integer ovarian_cancer_age;

    /**
     * Ovarian cancer occurrence indicator.
     */
    public String ovarian_cancer;

    /**
     * Age at pancreatic cancer diagnosis.
     */
    public Integer pancreatic_cancer_age;

    /**
     * Pancreatic cancer occurrence indicator.
     */
    public String pancreatic_cancer;

    /**
     * Age at prostate cancer diagnosis.
     */
    public Integer prostate_cancer_age;

    /**
     * Prostate cancer occurrence indicator.
     */
    public String prostate_cancer;

    /**
     * Race/ethnicity information.
     */
    public String race;

    /**
     * Numeric relationship code indicating family relationship type.
     */
    public Integer relationship;

    /**
     * Shared parent indicator for sibling relationships.
     */
    public String shared_parent;

    /**
     * Type of sibling relationship (full, half, step).
     */
    public String sibling_type;

    /**
     * Survey step identifier.
     */
    public String step;

    /**
     * Age at stomach cancer diagnosis.
     */
    public Integer stomach_cancer_age;

    /**
     * Stomach cancer occurrence indicator.
     */
    public String stomach_cancer;

    /**
     * Age at testicular cancer diagnosis.
     */
    public Integer testicular_cancer_age;

    /**
     * Testicular cancer occurrence indicator.
     */
    public String testicular_cancer;

    /**
     * Age at thyroid cancer diagnosis.
     */
    public Integer thyroid_cancer_age;

    /**
     * Thyroid cancer occurrence indicator.
     */
    public String thyroid_cancer;

    /**
     * Triple-negative breast cancer indicator.
     */
    public String triple_negative_breast_cancer;

    /**
     * Age at unknown cancer type diagnosis.
     */
    public Integer unknown_cancer_age;

    /**
     * Unknown cancer type occurrence indicator.
     */
    public String unknown_cancer;

    /**
     * Vital status of the family member (alive, deceased, unknown).
     */
    public String vital_status;

    /**
     * Cached title value combining step and step instance.
     * Transient field not persisted to the database.
     */
    @Transient
    private String title;

    /**
     * Gets the title for this fact record.
     * Title combines the step name with the step instance number if greater than 0.
     *
     * @return the formatted title string
     */
    public String getTitle() {
        if (title == null) {
            title = this.step;
            if (step_instance > 0) {
                title += " " + step_instance;
            }
            title = title;
        }
        return title;
    }

    /**
     * Gets the list of cancer diagnosis rows for this family member.
     * Creates a row for each cancer type that has been diagnosed, including
     * the cancer type name and age at diagnosis.
     *
     * @return list of Row objects representing cancer diagnoses
     */
    public List<Row> getRows() {
        List<Row> rows = new ArrayList<>();
        title = getTitle();
        //Create a row for each cancer
        if (TRUE.equalsIgnoreCase(bladder_cancer)) {
            rows.add(new Row(title, "Bladder Cancer", bladder_cancer_age));
        }

        if (multiple_bladder_cancers != null) {
            rows.add(new Row(title, "Multiple Bladder Cancers", multiple_bladder_cancers));
        }

        if (TRUE.equalsIgnoreCase(breast_cancer)) {
            rows.add(new Row(title, "Breast Cancer", breast_cancer_age));
            rows.add(new Row(title, "Triple Negative Breast Cancer", triple_negative_breast_cancer));
        }

        if (multiple_breast_cancers != null) {
            rows.add(new Row(title, "Multiple Breast Cancers", multiple_breast_cancers));
        }

        if (TRUE.equalsIgnoreCase(colon_or_rectal_cancer)) {
            rows.add(new Row(title, "Colon or Rectal Cancer", colon_or_rectal_cancer_age));
        }

        if (multiple_colon_or_rectal_cancers != null) {
            rows.add(new Row(title, "Multiple Colon or Rectal Cancers", multiple_colon_or_rectal_cancers));
        }

        if (TRUE.equalsIgnoreCase(endometrial_or_uterine_cancer)) {
            rows.add(new Row(title, "Endometrial or Uterine Cancer", endometrial_or_uterine_cancer_age));
        }

        if (multiple_endometrial_or_uterine_cancers != null) {
            rows.add(new Row(title, "Multiple Endometrial or Uterine Cancers", multiple_endometrial_or_uterine_cancers));
        }

        if (TRUE.equalsIgnoreCase(kidney_renal_cell_cancer)) {
            rows.add(new Row(title, "Kidney Renal Cell Cancer", kidney_renal_cell_cancer_age));
        }

        if (multiple_kidney_renal_cell_cancers != null) {
            rows.add(new Row(title, "Multiple Kidney Renal Cell Cancers", multiple_kidney_renal_cell_cancers));
        }

        if (TRUE.equalsIgnoreCase(leukemia)) {
            rows.add(new Row(title, "Leukemia", leukemia_age));
        }

        if (multiple_leukemias != null) {
            rows.add(new Row(title, "Multiple Leukemias", multiple_leukemias));
        }

        if (TRUE.equalsIgnoreCase(lung_cancer)) {
            rows.add(new Row(title, "Lung Cancer", lung_cancer_age));
        }

        if (multiple_lung_cancers != null) {
            rows.add(new Row(title, "Multiple Lung Cancers", multiple_lung_cancers));
        }

        if (TRUE.equalsIgnoreCase(lymphoma)) {
            rows.add(new Row(title, "Lymphoma", lymphoma_age));
        }

        if (multiple_lymphomas != null) {
            rows.add(new Row(title, "Multiple Lymphomas", multiple_lymphomas));
        }

        if (TRUE.equalsIgnoreCase(melanoma_skin_cancer)) {
            rows.add(new Row(title, "Melanoma Skin Cancer", melanoma_skin_cancer_age));
        }

        if (multiple_melanoma_skin_cancers != null) {
            rows.add(new Row(title, "Multiple Melanoma Skin Cancers", multiple_melanoma_skin_cancers));
        }

        if (TRUE.equalsIgnoreCase(nonmelanoma_skin_cancer)) {
            rows.add(new Row(title, "Non-Melanoma Skin Cancer", nonmelanoma_skin_cancer_age));
        }

        if (multiple_nonmelanoma_skin_cancers != null) {
            rows.add(new Row(title, "Multiple Non-Melanoma Skin Cancers", multiple_nonmelanoma_skin_cancers));
        }

        if (TRUE.equalsIgnoreCase(oral_cavity_or_throat_cancer)) {
            rows.add(new Row(title, "Oral Cavity or Throat Cancer", oral_cavity_or_throat_cancer_age));
        }

        if (multiple_oral_cavity_or_throat_cancers != null) {
            rows.add(new Row(title, "Multiple Oral Cavity or Throat Cancers", multiple_oral_cavity_or_throat_cancers));
        }

        if (TRUE.equalsIgnoreCase(other_cancer)) {

            String cancerName = "Other Cancer";
            if (other_cancer_name != null) {
                cancerName = other_cancer_name;
            }
            rows.add(new Row(title, cancerName, other_age));
        }

        if (multiple_other_cancers != null) {
            String cancerName = "Other Cancer";
            if (other_cancer_name != null) {
                cancerName = other_cancer_name;
            }
            rows.add(new Row(title, "Multiple cancerName" + 's', multiple_other_cancers));
        }

        if (TRUE.equalsIgnoreCase(ovarian_cancer)) {
            rows.add(new Row(title, "Ovarian Cancer", ovarian_cancer_age));
        }

        if (multiple_ovarian_cancers != null) {
            rows.add(new Row(title, "Multiple Ovarian Cancers", multiple_ovarian_cancers));
        }

        if (TRUE.equalsIgnoreCase(pancreatic_cancer)) {
            rows.add(new Row(title, "Pancreatic Cancer", pancreatic_cancer_age));
        }

        if (multiple_pancreatic_cancers != null) {
            rows.add(new Row(title, "Multiple Pancreatic Cancers", multiple_pancreatic_cancers));
        }

        if (TRUE.equalsIgnoreCase(prostate_cancer)) {
            rows.add(new Row(title, "Prostate Cancer", prostate_cancer_age));
        }

        if (multiple_prostate_cancers != null) {
            rows.add(new Row(title, "Multiple Prostate Cancers", multiple_prostate_cancers));
        }

        if (TRUE.equalsIgnoreCase(stomach_cancer)) {
            rows.add(new Row(title, "Stomach Cancer", stomach_cancer_age));
        }

        if (multiple_stomach_cancers != null) {
            rows.add(new Row(title, "Multiple Stomach Cancers", multiple_stomach_cancers));
        }

        if (TRUE.equalsIgnoreCase(testicular_cancer)) {
            rows.add(new Row(title, "Testicular Cancer", testicular_cancer_age));
        }

        if (multiple_testicular_cancers != null) {
            rows.add(new Row(title, "Multiple Testicular Cancers", multiple_testicular_cancers));
        }

        if (TRUE.equalsIgnoreCase(thyroid_cancer)) {
            rows.add(new Row(title, "Thyroid Cancer", thyroid_cancer_age));
        }

        if (multiple_thyroid_cancers != null) {
            rows.add(new Row(title, "Multiple Thyroid Cances", multiple_thyroid_cancers));
        }

        if (TRUE.equalsIgnoreCase(unknown_cancer)) {
            rows.add(new Row(title, "Unknown Cancer", unknown_cancer_age));
        }

        return rows;
    }

    /**
     * Gets the list of rows for the proband (main respondent) including Ashkenazi ancestry.
     * This method includes all cancer rows plus the Ashkenazi ancestry indicator.
     *
     * @return list of Row objects for the proband
     */
    public List<Row> getProbandRows() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(title, "Ashkenazi", ashkenazi));
        rows.addAll(getRows());
        return rows;
    }
}
