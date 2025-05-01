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
@Entity
@Table(name = "fact_fhhs_view", schema = "surveyreport")
public class FactFHHSView extends PanacheEntityBase {
    @Transient
    private static final String TRUE = "true";

    @Id
    public long id;
    public long respondent_id;
    public String name;
    public Integer step_instance;
    public Integer age;
    public String ashkenazi;
    public Integer bladder_cancer_age;
    public String bladder_cancer;
    public Integer breast_cancer_age;
    public String breast_cancer;
    public Integer colon_or_rectal_cancer_age;
    public String colon_or_rectal_cancer;
    public Integer endometrial_or_uterine_cancer_age;
    public String endometrial_or_uterine_cancer;
    public String gender;
    public String generation;
    public Integer kidney_renal_cell_cancer_age;
    public String kidney_renal_cell_cancer;
    public String latinx;
    public Integer leukemia_age;
    public String leukemia;
    public Integer lung_cancer_age;
    public String lung_cancer;
    public Integer lymphoma_age;
    public String lymphoma;
    public Integer melanoma_skin_cancer_age;
    public String melanoma_skin_cancer;
    public String multiple_bladder_cancers;
    public String multiple_breast_cancers;
    public String multiple_colon_or_rectal_cancers;
    public String multiple_endometrial_or_uterine_cancers;
    public String multiple_kidney_renal_cell_cancers;
    public String multiple_leukemias;
    public String multiple_lung_cancers;
    public String multiple_lymphomas;
    public String multiple_melanoma_skin_cancers;
    public String multiple_nonmelanoma_skin_cancers;
    public String multiple_oral_cavity_or_throat_cancers;
    public String multiple_other_cancers;
    public String multiple_ovarian_cancers;
    public String multiple_pancreatic_cancers;
    public String multiple_prostate_cancers;
    public String multiple_stomach_cancers;
    public String multiple_testicular_cancers;
    public String multiple_thyroid_cancers;
    public Integer nonmelanoma_skin_cancer_age;
    public String nonmelanoma_skin_cancer;
    public Integer oral_cavity_or_throat_cancer_age;
    public String oral_cavity_or_throat_cancer;
    public Integer other_age;
    public String other_cancer;
    public String other_cancer_name;
    public Integer ovarian_cancer_age;
    public String ovarian_cancer;
    public Integer pancreatic_cancer_age;
    public String pancreatic_cancer;
    public Integer prostate_cancer_age;
    public String prostate_cancer;
    public String race;
    public Integer relationship;
    public String shared_parent;
    public String sibling_type;
    public String step;
    public Integer stomach_cancer_age;
    public String stomach_cancer;
    public Integer testicular_cancer_age;
    public String testicular_cancer;
    public Integer thyroid_cancer_age;
    public String thyroid_cancer;
    public String triple_negative_breast_cancer;
    public Integer unknown_cancer_age;
    public String unknown_cancer;
    public String vital_status;

    @Transient
    private String title;

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
    public List<Row> getRows() {
        List<Row> rows = new ArrayList<>();
        title = getTitle();
        //Create a row for each cancer
        if (TRUE.equalsIgnoreCase(bladder_cancer)) {
            rows.add(new Row(title, "Blader Cancer", bladder_cancer_age));
        }

        if (multiple_bladder_cancers != null) {
            rows.add(new Row(title, "Multiple Blader Cancers", multiple_bladder_cancers));
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
    public List<Row> getProbandRows() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(title, "Ashkenazi", ashkenazi));
        rows.addAll(getRows());
        return rows;
    }
}
