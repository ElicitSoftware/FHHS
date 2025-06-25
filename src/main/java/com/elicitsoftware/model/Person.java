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

/**
 * Represents a person in the family history health survey system.
 * <p>
 * This class models an individual participant or family member with comprehensive
 * health information including demographics, relationships, and detailed cancer
 * history. It maintains both individual cancer occurrence data and multiple
 * cancer occurrence indicators for various cancer types.
 * </p>
 * <p>
 * The class integrates with a FamilyMember object to maintain consistency
 * between person-level and family-level data structures.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class Person {

    /**
     * The relationship of this person to the proband/main respondent.
     */
    private String Relationship;

    /**
     * Associated family member object for maintaining family-level data consistency.
     */
    private final FamilyMember familyMember = new FamilyMember();

    /**
     * Step instance identifier for tracking survey progression.
     */
    private long StepInstance;

    /**
     * Age of the person.
     */
    private String Age;

    /**
     * Ashkenazi Jewish ancestry indicator.
     */
    private String Ashkenazi;

    // Cancer history fields
    /**
     * Bladder cancer occurrence indicator.
     */
    private String Bladder_Cancer;

    /**
     * Breast cancer occurrence indicator.
     */
    private String Breast_Cancer;

    /**
     * Colorectal cancer occurrence indicator.
     */
    private String Colon_Rectal_Cancer;

    /**
     * Endometrial/uterine cancer occurrence indicator.
     */
    private String Endometrial_Uterine_Cancer;

    /**
     * Gender of the person.
     */
    private String Gender;

    /**
     * Kidney/renal cell cancer occurrence indicator.
     */
    private String Kidney_Renal_Cell_Cancer;

    /**
     * Leukemia cancer occurrence indicator.
     */
    private String Leukemia_Cancer;

    /**
     * Lung cancer occurrence indicator.
     */
    private String Lung_Cancer;

    /**
     * Lymphoma occurrence indicator.
     */
    private String Lymphoma;

    /**
     * Melanoma cancer occurrence indicator.
     */
    private String Melanoma_Cancer;

    /**
     * Non-melanoma cancer occurrence indicator.
     */
    private String Non_Melanoma_Cancer;

    /**
     * Oral/throat cancer occurrence indicator.
     */
    private String Oral_Throat_Cancer;

    /**
     * Other cancer occurrence indicator.
     */
    private String Other_Cancer;

    /**
     * Ovarian cancer occurrence indicator.
     */
    private String Ovarian_Cancer;

    /**
     * Specific type of other cancer if applicable.
     */
    private String Other_Cancer_Type;

    /**
     * Pancreatic cancer occurrence indicator.
     */
    private String Pancreatic_Cancer;

    /**
     * Prostate cancer occurrence indicator.
     */
    private String Prostate_Cancer;

    /**
     * Shared parent indicator for family relationship tracking.
     */
    private String SharedParent;

    /**
     * Stomach cancer occurrence indicator.
     */
    private String Stomach_Cancer;

    /**
     * Testicular cancer occurrence indicator.
     */
    private String Testicular_Cancer;

    /**
     * Thyroid cancer occurrence indicator.
     */
    private String Thyroid_Cancer;

    /**
     * Unknown cancer type occurrence indicator.
     */
    private String Unknown_Cancer;

    // Multiple cancer occurrence fields
    /**
     * Multiple bladder cancer occurrences indicator.
     */
    private String Multiple_Bladder_Cancer;

    /**
     * Multiple breast cancer occurrences indicator.
     */
    private String Multiple_Breast_Cancer;

    /**
     * Multiple colorectal cancer occurrences indicator.
     */
    private String Multiple_Colon_Rectal_Cancer;

    /**
     * Multiple endometrial/uterine cancer occurrences indicator.
     */
    private String Multiple_Endometrial_Uterine_Cancer;

    /**
     * Multiple kidney/renal cell cancer occurrences indicator.
     */
    private String Multiple_Kidney_Renal_Cell_Cancer;

    /**
     * Multiple leukemia occurrences indicator.
     */
    private String Multiple_Leukemia_Cancer;

    /**
     * Multiple lung cancer occurrences indicator.
     */
    private String Multiple_Lung_Cancer;

    /**
     * Multiple lymphoma occurrences indicator.
     */
    private String Multiple_Lymphoma;

    /**
     * Multiple melanoma occurrences indicator.
     */
    private String Multiple_Melanoma_Cancer;

    /**
     * Multiple non-melanoma occurrences indicator.
     */
    private String Multiple_Non_Melanoma_Cancer;

    /**
     * Multiple oral/throat cancer occurrences indicator.
     */
    private String Multiple_Oral_Throat_Cancer;

    /**
     * Multiple other cancer occurrences indicator.
     */
    private String Multiple_Other_Cancer;

    /**
     * Multiple ovarian cancer occurrences indicator.
     */
    private String Multiple_Ovarian_Cancer;

    /**
     * Multiple pancreatic cancer occurrences indicator.
     */
    private String Multiple_Pancreatic_Cancer;

    /**
     * Multiple prostate cancer occurrences indicator.
     */
    private String Multiple_Prostate_Cancer;

    /**
     * Multiple stomach cancer occurrences indicator.
     */
    private String Multiple_Stomach_Cancer;

    /**
     * Multiple testicular cancer occurrences indicator.
     */
    private String Multiple_Testicular_Cancer;

    /**
     * Multiple thyroid cancer occurrences indicator.
     */
    private String Multiple_Thyroid_Cancer;

    private String Vital_Status;


    public String getGender() {
        return Gender;
    }

    public void setGender(String gender) {
        if(gender != null && !gender.isEmpty()) {
            Gender = gender.toLowerCase();
            switch (gender.toLowerCase()) {
                case "male":
                    this.familyMember.Sex = 1;
                    break;
                case "female":
                    this.familyMember.Sex = 2;
                    break;
                case "other":
                    this.familyMember.Sex = 3;
                    break;
            }
        }
    }

    public void setVital_Status(String vital_Status) {
        if(vital_Status != null && !vital_Status.isEmpty()) {
            Vital_Status = vital_Status.toLowerCase();
            switch (vital_Status.toLowerCase()) {
                case "alive":
                    this.familyMember.Status = 0;
                    break;
                case "deceased":
                    this.familyMember.Status = 1;
                    break;
            }
        }
    }

    public FamilyMember getFamilyMember() {
        return this.familyMember;
    }

    public String getAge() {
        return Age;
    }

    public void setAge(Integer age) {
        if(age != null) {
            this.familyMember.Age = age.toString();
            Age = age.toString();
        }
    }

    public void setAshkenazi(String ashkenazi) {
        if(ashkenazi != null && !ashkenazi.isEmpty()) {
            Ashkenazi = ashkenazi;
            this.familyMember.Ashkenazi = ashkenazi.replace(' ', '_');
        }
    }


    public void setBladder_Cancer(String bladder_Cancer) {
        Bladder_Cancer = bladder_Cancer;
        this.familyMember.Bladder_Cancer = bladder_Cancer;
    }

    public String getSharedParent() {
        return SharedParent;
    }

    public void setSharedParent(String sharedParent) {
        SharedParent = sharedParent;
    }

    public void setBreast_Cancer(String breast_Cancer) {
        Breast_Cancer = breast_Cancer;
        this.familyMember.Breast_Cancer = breast_Cancer;
    }

    public void setColon_Rectal_Cancer(String colon_Rectal_Cancer) {
        Colon_Rectal_Cancer = colon_Rectal_Cancer;
        this.familyMember.Colon_Rectal_Cancer = colon_Rectal_Cancer;
    }


    public void setEndometrial_Uterine_Cancer(String endometrial_Uterine_Cancer) {
        Endometrial_Uterine_Cancer = endometrial_Uterine_Cancer;
        this.familyMember.Endometrial_Uterine_Cancer = endometrial_Uterine_Cancer;
    }

    public void setKidney_Renal_Cell_Cancer(String kidney_Renal_Cell_Cancer) {
        Kidney_Renal_Cell_Cancer = kidney_Renal_Cell_Cancer;
        this.familyMember.Kidney_Renal_Cell_Cancer = kidney_Renal_Cell_Cancer;
    }

    public void setLeukemia_Cancer(String leukemia_Cancer) {
        Leukemia_Cancer = leukemia_Cancer;
        this.familyMember.Leukemia_Cancer = leukemia_Cancer;
    }

    public void setLung_Cancer(String lung_Cancer) {
        Lung_Cancer = lung_Cancer;
        this.familyMember.Lung_Cancer = lung_Cancer;
    }

    public void setLymphoma(String lymphoma) {
        Lymphoma = lymphoma;
        this.familyMember.Lymphoma = lymphoma;
    }

    public void setMelanoma_Cancer(String melanoma_Cancer) {
        Melanoma_Cancer = melanoma_Cancer;
        this.familyMember.Melanoma_Cancer = melanoma_Cancer;
    }

    public void setNon_Melanoma_Cancer(String non_Melanoma_Cancer) {
        Non_Melanoma_Cancer = non_Melanoma_Cancer;
        this.familyMember.Non_Melanoma_Cancer = non_Melanoma_Cancer;
    }

    public void setOral_Throat_Cancer(String oral_Throat_Cancer) {
        Oral_Throat_Cancer = oral_Throat_Cancer;
        this.familyMember.Oral_Throat_Cancer = oral_Throat_Cancer;
    }

    public void setOther_Cancer(String other_Cancer) {
        Other_Cancer = other_Cancer;
        this.familyMember.Other_Cancer = other_Cancer;
    }

    public void setOther_Cancer_Type(String type) {
        Other_Cancer_Type = type;
        this.familyMember.Other_Cancer_Type = type;
    }

    public void setOvarian_Cancer(String ovarian_Cancer) {
        Ovarian_Cancer = ovarian_Cancer;
        this.familyMember.Ovarian_Cancer = ovarian_Cancer;
    }

    public void setPancreatic_Cancer(String pancreatic_Cancer) {
        Pancreatic_Cancer = pancreatic_Cancer;
        this.familyMember.Pancreatic_Cancer = pancreatic_Cancer;
    }

    public void setProstate_Cancer(String prostate_Cancer) {
        Prostate_Cancer = prostate_Cancer;
        this.familyMember.Prostate_Cancer = prostate_Cancer;
    }

    public void setStomach_Cancer(String stomach_Cancer) {
        Stomach_Cancer = stomach_Cancer;
        this.familyMember.Stomach_Cancer = stomach_Cancer;
    }

    public void setTesticular_Cancer(String testicular_Cancer) {
        Testicular_Cancer = testicular_Cancer;
        this.familyMember.Testicular_Cancer = testicular_Cancer;
    }

    public void setThyroid_Cancer(String thyroid_Cancer) {
        Thyroid_Cancer = thyroid_Cancer;
        this.familyMember.Thyroid_Cancer = thyroid_Cancer;
    }

    public void setUnknown_Cancer(String unknown_Cancer) {
        Unknown_Cancer = unknown_Cancer;
        this.familyMember.Unknown_Cancer = unknown_Cancer;
    }

    public void setMultiple_Bladder_Cancer(String multiple_Bladder_Cancer) {
        Multiple_Bladder_Cancer = multiple_Bladder_Cancer;
        this.familyMember.Multiple_Bladder_Cancer = multiple_Bladder_Cancer;
    }

    public void setMultiple_Breast_Cancer(String multiple_Breast_Cancer) {
        Multiple_Breast_Cancer = multiple_Breast_Cancer;
        this.familyMember.Multiple_Breast_Cancer = multiple_Breast_Cancer;
    }

    public void setMultiple_Colon_Rectal_Cancer(String multiple_Colon_Rectal_Cancer) {
        Multiple_Colon_Rectal_Cancer = multiple_Colon_Rectal_Cancer;
        this.familyMember.Multiple_Colon_Rectal_Cancer = multiple_Colon_Rectal_Cancer;
    }

    public void setMultiple_Endometrial_Uterine_Cancer(String multiple_Endometrial_Uterine_Cancer) {
        Multiple_Endometrial_Uterine_Cancer = multiple_Endometrial_Uterine_Cancer;
        this.familyMember.Multiple_Endometrial_Uterine_Cancer = multiple_Endometrial_Uterine_Cancer;
    }

    public void setMultiple_Kidney_Renal_Cell_Cancer(String multiple_Kidney_Renal_Cell_Cancer) {
        Multiple_Kidney_Renal_Cell_Cancer = multiple_Kidney_Renal_Cell_Cancer;
        this.familyMember.Multiple_Kidney_Renal_Cell_Cancer = multiple_Kidney_Renal_Cell_Cancer;
    }

    public void setMultiple_Leukemia_Cancer(String multiple_Leukemia_Cancer) {
        Multiple_Leukemia_Cancer = multiple_Leukemia_Cancer;
        this.familyMember.Multiple_Leukemia_Cancer = multiple_Leukemia_Cancer;
    }

    public void setMultiple_Lung_Cancer(String multiple_Lung_Cancer) {
        Multiple_Lung_Cancer = multiple_Lung_Cancer;
        this.familyMember.Multiple_Lung_Cancer = multiple_Lung_Cancer;
    }

    public void setMultiple_Lymphoma(String multiple_Lymphoma) {
        Multiple_Lymphoma = multiple_Lymphoma;
        this.familyMember.Multiple_Lymphoma = multiple_Lymphoma;
    }

    public void setMultiple_Melanoma_Cancer(String multiple_Melanoma_Cancer) {
        Multiple_Melanoma_Cancer = multiple_Melanoma_Cancer;
        this.familyMember.Multiple_Melanoma_Cancer = multiple_Melanoma_Cancer;
    }

    public void setMultiple_Non_Melanoma_Cancer(String multiple_Non_Melanoma_Cancer) {
        Multiple_Non_Melanoma_Cancer = multiple_Non_Melanoma_Cancer;
        this.familyMember.Multiple_Non_Melanoma_Cancer = multiple_Non_Melanoma_Cancer;
    }

    public void setMultiple_Oral_Throat_Cancer(String multiple_Oral_Throat_Cancer) {
        Multiple_Oral_Throat_Cancer = multiple_Oral_Throat_Cancer;
        this.familyMember.Multiple_Oral_Throat_Cancer = multiple_Oral_Throat_Cancer;
    }

    public void setMultiple_Other_Cancer(String multiple_Other_Cancer) {
        Multiple_Other_Cancer = multiple_Other_Cancer;
        this.familyMember.Multiple_Other_Cancer = multiple_Other_Cancer;
    }

    public void setMultiple_Ovarian_Cancer(String multiple_Ovarian_Cancer) {
        Multiple_Ovarian_Cancer = multiple_Ovarian_Cancer;
        this.familyMember.Multiple_Ovarian_Cancer = multiple_Ovarian_Cancer;
    }

    public void setMultiple_Pancreatic_Cancer(String multiple_Pancreatic_Cancer) {
        Multiple_Pancreatic_Cancer = multiple_Pancreatic_Cancer;
        this.familyMember.Multiple_Pancreatic_Cancer = multiple_Pancreatic_Cancer;
    }

    public void setMultiple_Prostate_Cancer(String multiple_Prostate_Cancer) {
        Multiple_Prostate_Cancer = multiple_Prostate_Cancer;
        this.familyMember.Multiple_Prostate_Cancer = multiple_Prostate_Cancer;
    }

    public void setMultiple_Stomach_Cancer(String multiple_Stomach_Cancer) {
        Multiple_Stomach_Cancer = multiple_Stomach_Cancer;
        this.familyMember.Multiple_Stomach_Cancer = multiple_Stomach_Cancer;
    }

    public void setMultiple_Testicular_Cancer(String multiple_Testicular_Cancer) {
        Multiple_Testicular_Cancer = multiple_Testicular_Cancer;
        this.familyMember.Multiple_Testicular_Cancer = multiple_Testicular_Cancer;
    }

    public void setMultiple_Thyroid_Cancer(String multiple_Thyroid_Cancer) {
        Multiple_Thyroid_Cancer = multiple_Thyroid_Cancer;
        this.familyMember.Multiple_Thyroid_Cancer = multiple_Thyroid_Cancer;
    }
}
