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
 * Represents an individual family member in the family history health survey system.
 * <p>
 * This class models a family member with comprehensive health information including
 * demographics, family relationships, cancer history, and pedigree positioning data.
 * It supports detailed cancer tracking with both occurrence indicators and multiple
 * cancer occurrence flags for complex medical histories.
 * </p>
 * <p>
 * The class integrates with pedigree generation systems by providing formatted
 * output that includes quadrant-based cancer indicators used in family tree
 * visualizations.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class FamilyMember {

    /**
     * Unique identifier for this family member within the family structure.
     */
    public int ID;

    /**
     * Gender/sex indicator using numeric codes.
     * Default value is 3 (other/unknown).
     */
    public int Sex = 3;

    /**
     * Age of the family member as a string.
     */
    public String Age = "";

    /**
     * ID reference to this member's father.
     * 0 indicates no father information available.
     */
    public int Dadid = 0;

    /**
     * ID reference to this member's mother.
     * 0 indicates no mother information available.
     */
    public int Momid = 0;

    /**
     * Shared parent indicator for tracking sibling relationships.
     */
    public int SharedParent;

    /**
     * Vital status indicator (alive, deceased, unknown).
     */
    public int Status;

    // Cancer occurrence fields
    /**
     * Bladder cancer occurrence and age information.
     */
    public String Bladder_Cancer;

    /**
     * Breast cancer occurrence and age information.
     */
    public String Breast_Cancer;

    /**
     * Colorectal cancer occurrence and age information.
     */
    public String Colon_Rectal_Cancer;

    /**
     * Endometrial/uterine cancer occurrence and age information.
     */
    public String Endometrial_Uterine_Cancer;

    /**
     * Kidney/renal cell cancer occurrence and age information.
     */
    public String Kidney_Renal_Cell_Cancer;

    /**
     * Leukemia occurrence and age information.
     */
    public String Leukemia_Cancer;

    /**
     * Lung cancer occurrence and age information.
     */
    public String Lung_Cancer;

    /**
     * Lymphoma occurrence and age information.
     */
    public String Lymphoma;

    /**
     * Melanoma cancer occurrence and age information.
     */
    public String Melanoma_Cancer;

    /**
     * Non-melanoma cancer occurrence and age information.
     */
    public String Non_Melanoma_Cancer;

    /**
     * Oral/throat cancer occurrence and age information.
     */
    public String Oral_Throat_Cancer;

    /**
     * Other cancer occurrence and age information.
     */
    public String Other_Cancer;

    /**
     * Specific type of other cancer if applicable.
     */
    public String Other_Cancer_Type = "";

    /**
     * Ovarian cancer occurrence and age information.
     */
    public String Ovarian_Cancer;

    /**
     * Pancreatic cancer occurrence and age information.
     */
    public String Pancreatic_Cancer;

    /**
     * Prostate cancer occurrence and age information.
     */
    public String Prostate_Cancer;

    /**
     * Stomach cancer occurrence and age information.
     */
    public String Stomach_Cancer;

    /**
     * Testicular cancer occurrence and age information.
     */
    public String Testicular_Cancer;

    /**
     * Thyroid cancer occurrence and age information.
     */
    public String Thyroid_Cancer;

    /**
     * Unknown cancer type occurrence and age information.
     */
    public String Unknown_Cancer;

    /**
     * Ashkenazi Jewish ancestry indicator.
     */
    public String Ashkenazi;

    /**
     * Flag indicating if this is an unknown/placeholder family member.
     */
    public boolean unknown;

    /**
     * Name of the family member.
     */
    public String name;

    // Multiple cancer occurrence fields
    /**
     * Multiple bladder cancer occurrences indicator.
     */
    public String Multiple_Bladder_Cancer;

    /**
     * Multiple breast cancer occurrences indicator.
     */
    public String Multiple_Breast_Cancer;

    /**
     * Multiple colorectal cancer occurrences indicator.
     */
    public String Multiple_Colon_Rectal_Cancer;

    /**
     * Multiple endometrial/uterine cancer occurrences indicator.
     */
    public String Multiple_Endometrial_Uterine_Cancer;

    /**
     * Multiple kidney/renal cell cancer occurrences indicator.
     */
    public String Multiple_Kidney_Renal_Cell_Cancer;

    /**
     * Multiple leukemia occurrences indicator.
     */
    public String Multiple_Leukemia_Cancer;

    /**
     * Multiple lung cancer occurrences indicator.
     */
    public String Multiple_Lung_Cancer;

    /**
     * Multiple lymphoma occurrences indicator.
     */
    public String Multiple_Lymphoma;

    /**
     * Multiple melanoma cancer occurrences indicator.
     */
    public String Multiple_Melanoma_Cancer;

    /**
     * Multiple non-melanoma cancer occurrences indicator.
     */
    public String Multiple_Non_Melanoma_Cancer;

    /**
     * Multiple oral/throat cancer occurrences indicator.
     */
    public String Multiple_Oral_Throat_Cancer;

    /**
     * Multiple other cancer occurrences indicator.
     */
    public String Multiple_Other_Cancer;

    /**
     * Multiple other cancer type specification.
     */
    public String Multiple_Other_Cancer_Type = "";

    /**
     * Multiple ovarian cancer occurrences indicator.
     */
    public String Multiple_Ovarian_Cancer;

    /**
     * Multiple pancreatic cancer occurrences indicator.
     */
    public String Multiple_Pancreatic_Cancer;

    /**
     * Multiple prostate cancer occurrences indicator.
     */
    public String Multiple_Prostate_Cancer;

    /**
     * Multiple stomach cancer occurrences indicator.
     */
    public String Multiple_Stomach_Cancer;

    /**
     * Multiple testicular cancer occurrences indicator.
     */
    public String Multiple_Testicular_Cancer;

    /**
     * Multiple thyroid cancer occurrences indicator.
     */
    public String Multiple_Thyroid_Cancer;

    /**
     * Default constructor for creating a new FamilyMember.
     */
    public FamilyMember() {
        super();
    }

    /**
     * Generates a pedigree-formatted string representation of this family member.
     * <p>
     * Creates a tab-delimited string containing:
     * <ul>
     *   <li>Pedigree ID, member ID, sex, parent IDs, and vital status</li>
     *   <li>Quadrant-based cancer indicators for visual representation</li>
     *   <li>Multiple cancer occurrence markers if applicable</li>
     * </ul>
     *
     * The quadrant system organizes cancers by type for visual pedigree display:
     * <ul>
     *   <li>Upper Left: Bladder, Breast, Kidney, Melanoma, Non-melanoma</li>
     *   <li>Upper Right: Colorectal, Oral/throat, Pancreatic, Leukemia</li>
     *   <li>Lower Left: Endometrial, Prostate, Thyroid, Lung, Lymphoma</li>
     *   <li>Lower Right: Ovarian, Stomach, Testicular, Other, Unknown</li>
     * </ul>
     *
     * @return a formatted string suitable for pedigree generation tools
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(1 + "	");
        sb.append(this.ID).append("	");
        sb.append(this.Sex).append("	");
        sb.append(this.Dadid).append("	");
        sb.append(this.Momid).append("	");
        sb.append(this.Status).append("	");
        sb.append(getID());
        if (this.ID > 0 && !this.unknown) {
            // Upper Left Quadrent
            if (this.Bladder_Cancer != null || this.Breast_Cancer != null || this.Kidney_Renal_Cell_Cancer != null || this.Melanoma_Cancer
                    != null || this.Non_Melanoma_Cancer != null) {
                sb.append("1	");
            } else {
                sb.append("0	");
            }

            // Upper Right Quadrent
            if (this.Colon_Rectal_Cancer != null || this.Oral_Throat_Cancer != null || this.Pancreatic_Cancer != null
                    || this.Leukemia_Cancer != null) {
                sb.append("1	");
            } else {
                sb.append("0	");
            }

            // Lower Left Quadrent
            if (this.Endometrial_Uterine_Cancer != null || this.Prostate_Cancer != null || this.Thyroid_Cancer != null
                    || this.Lung_Cancer != null || this.Lymphoma != null) {
                sb.append("1	");
            } else {
                sb.append("0	");
            }

            // Lower Right Quadrent
            if (this.Ovarian_Cancer != null || this.Stomach_Cancer != null || this.Testicular_Cancer != null || this.Other_Cancer
                    != null || this.Unknown_Cancer != null) {
                sb.append("1");
            } else {
                sb.append("0");
            }
        } else {
            // Unknown person and disease
            sb.append("NA	NA	NA	NA");
        }

        sb.append(System.lineSeparator());
        return sb.toString();
    }

    private String getID() {
        StringBuilder sb = new StringBuilder(this.name);

        if (this.Age != null && !this.Age.isEmpty()) {
            sb.append("-Age_").append(this.Age);
        }

        if (this.Bladder_Cancer != null) {
            if (this.Multiple_Bladder_Cancer != null && this.Multiple_Bladder_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Bladder*_").append(this.Bladder_Cancer);
            } else {
                sb.append("-Bladder_").append(this.Bladder_Cancer);
            }
        }

        if (this.Breast_Cancer != null) {
            if (this.Multiple_Breast_Cancer != null && this.Multiple_Breast_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Breast*_").append(this.Breast_Cancer);
            } else {
                sb.append("-Breast_").append(this.Breast_Cancer);
            }
        }

        if (this.Colon_Rectal_Cancer != null) {
            if (this.Multiple_Colon_Rectal_Cancer != null && this.Multiple_Colon_Rectal_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Colon*_").append(this.Colon_Rectal_Cancer);
            } else {
                sb.append("-Colon_").append(this.Colon_Rectal_Cancer);
            }
        }

        if (this.Endometrial_Uterine_Cancer != null) {
            if (this.Multiple_Endometrial_Uterine_Cancer != null && this.Multiple_Endometrial_Uterine_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Uterine*_").append(this.Endometrial_Uterine_Cancer);
            } else {
                sb.append("-Uterine_").append(this.Endometrial_Uterine_Cancer);
            }
        }

        if (this.Kidney_Renal_Cell_Cancer != null) {
            if (this.Multiple_Kidney_Renal_Cell_Cancer != null && this.Multiple_Kidney_Renal_Cell_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Kidney*_").append(this.Kidney_Renal_Cell_Cancer);
            } else {
                sb.append("-Kidney_").append(this.Kidney_Renal_Cell_Cancer);
            }
        }

        if (this.Leukemia_Cancer != null) {
            if (this.Multiple_Leukemia_Cancer != null && this.Multiple_Leukemia_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Leukemia*_").append(this.Leukemia_Cancer);
            } else {
                sb.append("-Leukemia_").append(this.Leukemia_Cancer);
            }
        }

        if (this.Lung_Cancer != null) {
            if (this.Multiple_Lung_Cancer != null && this.Multiple_Lung_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Lung*_").append(this.Lung_Cancer);
            } else {
                sb.append("-Lung_").append(this.Lung_Cancer);
            }
        }

        if (this.Lymphoma != null) {
            if (this.Multiple_Lymphoma != null && this.Multiple_Lymphoma.equalsIgnoreCase("true")) {
                sb.append("-Lymphoma*_").append(this.Lymphoma);
            } else {
                sb.append("-Lymphoma_").append(this.Lymphoma);
            }
        }

        if (this.Melanoma_Cancer != null) {
            if (this.Multiple_Melanoma_Cancer != null && this.Multiple_Melanoma_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Melanoma*_").append(this.Melanoma_Cancer);
            } else {
                sb.append("-Melanoma_").append(this.Melanoma_Cancer);
            }
        }

        if (this.Non_Melanoma_Cancer != null) {
            if (this.Multiple_Non_Melanoma_Cancer != null && this.Multiple_Non_Melanoma_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Non-Melanoma*_").append(this.Non_Melanoma_Cancer);
            } else {
                sb.append("-Non-Melanoma_").append(this.Non_Melanoma_Cancer);
            }

        }

        if (this.Oral_Throat_Cancer != null) {
            if (this.Multiple_Oral_Throat_Cancer != null && this.Multiple_Oral_Throat_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Oral*_").append(this.Oral_Throat_Cancer);
            } else {
                sb.append("-Oral_").append(this.Oral_Throat_Cancer);
            }
        }

        if (this.Other_Cancer != null || !this.Other_Cancer_Type.isEmpty()) {
            if (this.Multiple_Other_Cancer != null && this.Multiple_Other_Cancer.equalsIgnoreCase("true")) {
                if (this.Other_Cancer != null) {
                    sb.append("-Other*_");
                    sb.append(this.Other_Cancer);
                }
                if (!this.Other_Cancer_Type.isEmpty()) {
                    sb.append("-Other_");
                    sb.append("_(").append(this.Other_Cancer_Type).append("*)");
                }
            } else {
                sb.append("-Other_");
                if (this.Other_Cancer != null) {
                    sb.append(this.Other_Cancer);
                }
                if (!this.Other_Cancer_Type.isEmpty()) {
                    sb.append("_(").append(this.Other_Cancer_Type).append(")");
                }
            }

        }

        if (this.Ovarian_Cancer != null) {
            if (this.Multiple_Ovarian_Cancer != null && this.Multiple_Ovarian_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Ovarian*_").append(this.Ovarian_Cancer);
            } else {
                sb.append("-Ovarian_").append(this.Ovarian_Cancer);
            }
        }

        if (this.Pancreatic_Cancer != null) {
            if (this.Multiple_Pancreatic_Cancer != null && this.Multiple_Pancreatic_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Pancreatic*_").append(this.Pancreatic_Cancer);
            } else {
                sb.append("-Pancreatic_").append(this.Pancreatic_Cancer);
            }
        }

        if (this.Prostate_Cancer != null) {
            if (this.Multiple_Prostate_Cancer != null && this.Multiple_Prostate_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Prostate*_").append(this.Prostate_Cancer);
            } else {
                sb.append("-Prostate_").append(this.Prostate_Cancer);
            }
        }

        if (this.Stomach_Cancer != null) {
            if (this.Multiple_Stomach_Cancer != null && this.Multiple_Stomach_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Stomach*_").append(this.Stomach_Cancer);
            } else {
                sb.append("-Stomach_").append(this.Stomach_Cancer);
            }
        }

        if (this.Testicular_Cancer != null) {
            if (this.Multiple_Testicular_Cancer != null && this.Multiple_Testicular_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Testicular*_").append(this.Testicular_Cancer);
            } else {
                sb.append("-Testicular_").append(this.Testicular_Cancer);
            }
        }

        if (this.Thyroid_Cancer != null) {
            if (this.Multiple_Thyroid_Cancer != null && this.Multiple_Thyroid_Cancer.equalsIgnoreCase("true")) {
                sb.append("-Thyroid*_").append(this.Thyroid_Cancer);
            } else {
                sb.append("-Thyroid_").append(this.Thyroid_Cancer);
            }
        }

        if (this.Unknown_Cancer != null) {
            sb.append("-Unknown_").append(this.Unknown_Cancer);
        }

        if (this.Ashkenazi != null) {
            if (this.Ashkenazi.equals("Both_Parents") || this.Ashkenazi.equals("Maternal") || this.Ashkenazi.equals("Paternal")) {
                sb.append("-Ashkenazi:_").append(this.Ashkenazi);
            }
        }
        sb.append("	");

        if (sb.length() > 75) {
            return sb.toString().replaceAll(" ", "_").substring(0, 75) + "  ";
        } else {
            return sb.toString().replaceAll(" ", "_");
        }

    }

    public boolean hasMultipleCancers() {
        if (Multiple_Bladder_Cancer != null && Multiple_Bladder_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Breast_Cancer != null && Multiple_Breast_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Colon_Rectal_Cancer != null && Multiple_Colon_Rectal_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Endometrial_Uterine_Cancer != null && Multiple_Endometrial_Uterine_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Kidney_Renal_Cell_Cancer != null && Multiple_Kidney_Renal_Cell_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Leukemia_Cancer != null && Multiple_Leukemia_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Lung_Cancer != null && Multiple_Lung_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Melanoma_Cancer != null && Multiple_Melanoma_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Non_Melanoma_Cancer != null && Multiple_Non_Melanoma_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Oral_Throat_Cancer != null && Multiple_Oral_Throat_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Other_Cancer != null && Multiple_Other_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Ovarian_Cancer != null && Multiple_Ovarian_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Lymphoma != null && Multiple_Lymphoma.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Pancreatic_Cancer != null && Multiple_Pancreatic_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Prostate_Cancer != null && Multiple_Prostate_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        if (Multiple_Stomach_Cancer != null && Multiple_Stomach_Cancer.equalsIgnoreCase("true")) {
            return true;
        }
        return Multiple_Thyroid_Cancer != null && Multiple_Thyroid_Cancer.equalsIgnoreCase("true");
    }
}
