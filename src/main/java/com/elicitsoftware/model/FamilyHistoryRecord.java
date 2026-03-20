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
 * Lightweight data transfer object for complete family history records.
 * <p>
 * Combines demographic and cancer history data for a single family member,
 * optimized for minimal data transfer and avoiding expensive ORM joins.
 * This record contains all information needed by FamilyManager for building
 * complete family structures without the performance overhead of loading
 * full dimension table relationships.
 * </p>
 * <p>
 * The record uses native SQL queries that select only necessary columns directly
 * from the fact view, bypassing foreign key relationships to dimension tables
 * that would normally trigger expensive cascading joins.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class FamilyHistoryRecord {
    // Demographic and relationship fields
    /**
     * Survey step identifier for this record.
     */
    public final String step;
    /**
     * Step instance identifier for tracking survey progression.
     */
    public final String stepInstance;
    /**
     * Relationship of the family member to the proband/main respondent.
     */
    public final String relationship;
    /**
     * Age of the family member.
     */
    public final Integer age;
    /**
     * Gender of the family member.
     */
    public final String gender;
    /**
     * Vital status (e.g., alive, deceased).
     */
    public final String vitalStatus;
    /**
     * Shared parent indicator for complex family relationships.
     */
    public final String sharedParent;
    /**
     * Ashkenazi Jewish ancestry indicator.
     */
    public final String ashkenazi;

    // Cancer history fields (56 total cancer-related columns)
    /**
     * Bladder cancer diagnosis indicator.
     */
    public final String bladderCancer;
    /**
     * Age at bladder cancer diagnosis.
     */
    public final Integer bladderCancerAge;
    /**
     * Breast cancer diagnosis indicator.
     */
    public final String breastCancer;
    /**
     * Age at breast cancer diagnosis.
     */
    public final Integer breastCancerAge;
    /**
     * Triple negative breast cancer indicator.
     */
    public final String tripleNegativeBreastCancer;
    /**
     * Colon or rectal cancer diagnosis indicator.
     */
    public final String colonOrRectalCancer;
    /**
     * Age at colon or rectal cancer diagnosis.
     */
    public final Integer colonOrRectalCancerAge;
    /**
     * Endometrial or uterine cancer diagnosis indicator.
     */
    public final String endometrialOrUterineCancer;
    /**
     * Age at endometrial or uterine cancer diagnosis.
     */
    public final Integer endometrialOrUterineCancerAge;
    /**
     * Kidney/renal cell cancer diagnosis indicator.
     */
    public final String kidneyRenalCellCancer;
    /**
     * Age at kidney/renal cell cancer diagnosis.
     */
    public final Integer kidneyRenalCellCancerAge;
    /**
     * Leukemia diagnosis indicator.
     */
    public final String leukemia;
    /**
     * Age at leukemia diagnosis.
     */
    public final Integer leukemiaAge;
    /**
     * Lung cancer diagnosis indicator.
     */
    public final String lungCancer;
    /**
     * Age at lung cancer diagnosis.
     */
    public final Integer lungCancerAge;
    /**
     * Lymphoma diagnosis indicator.
     */
    public final String lymphoma;
    /**
     * Age at lymphoma diagnosis.
     */
    public final Integer lymphomaAge;
    /**
     * Melanoma skin cancer diagnosis indicator.
     */
    public final String melanomaSkinCancer;
    /**
     * Age at melanoma skin cancer diagnosis.
     */
    public final Integer melanomaSkinCancerAge;
    /**
     * Nonmelanoma skin cancer diagnosis indicator.
     */
    public final String nonmelanomaSkinCancer;
    /**
     * Age at nonmelanoma skin cancer diagnosis.
     */
    public final Integer nonmelanomaSkinCancerAge;
    /**
     * Oral cavity or throat cancer diagnosis indicator.
     */
    public final String oralCavityOrThroatCancer;
    /**
     * Age at oral cavity or throat cancer diagnosis.
     */
    public final Integer oralCavityOrThroatCancerAge;
    /**
     * Other cancer diagnosis indicator.
     */
    public final String otherCancer;
    /**
     * Age at other cancer diagnosis.
     */
    public final Integer otherAge;
    /**
     * Name of other cancer diagnosed.
     */
    public final String otherCancerName;
    /**
     * Ovarian cancer diagnosis indicator.
     */
    public final String ovarianCancer;
    /**
     * Age at ovarian cancer diagnosis.
     */
    public final Integer ovarianCancerAge;
    /**
     * Pancreatic cancer diagnosis indicator.
     */
    public final String pancreaticCancer;
    /**
     * Age at pancreatic cancer diagnosis.
     */
    public final Integer pancreaticCancerAge;
    /**
     * Prostate cancer diagnosis indicator.
     */
    public final String prostateCancer;
    /**
     * Age at prostate cancer diagnosis.
     */
    public final Integer prostateCancerAge;
    /**
     * Stomach cancer diagnosis indicator.
     */
    public final String stomachCancer;
    /**
     * Age at stomach cancer diagnosis.
     */
    public final Integer stomachCancerAge;
    /**
     * Testicular cancer diagnosis indicator.
     */
    public final String testicularCancer;
    /**
     * Age at testicular cancer diagnosis.
     */
    public final Integer testicularCancerAge;
    /**
     * Thyroid cancer diagnosis indicator.
     */
    public final String thyroidCancer;
    /**
     * Age at thyroid cancer diagnosis.
     */
    public final Integer thyroidCancerAge;
    /**
     * Unknown cancer diagnosis indicator.
     */
    public final String unknownCancer;
    /**
     * Age at unknown cancer diagnosis.
     */
    public final Integer unknownCancerAge;
    /**
     * Multiple bladder cancers indicator.
     */
    public final String multipleBladdercancers;
    /**
     * Multiple breast cancers indicator.
     */
    public final String multipleBreastcancers;
    /**
     * Multiple colon or rectal cancers indicator.
     */
    public final String multipleColonOrRectalCancers;
    /**
     * Multiple endometrial or uterine cancers indicator.
     */
    public final String multipleEndometrialOrUterineCancers;
    /**
     * Multiple kidney renal cell cancers indicator.
     */
    public final String multipleKidneyRenalCellCancers;
    /**
     * Multiple leukemias indicator.
     */
    public final String multipleLeukemias;
    /**
     * Multiple lung cancers indicator.
     */
    public final String multipleLungCancers;
    /**
     * Multiple lymphomas indicator.
     */
    public final String multipleLymphomas;
    /**
     * Multiple melanoma skin cancers indicator.
     */
    public final String multipleMelanomaSkinCancers;
    /**
     * Multiple nonmelanoma skin cancers indicator.
     */
    public final String multipleNonmelanomaSkinCancers;
    /**
     * Multiple oral cavity or throat cancers indicator.
     */
    public final String multipleOralCavityOrThroatCancers;
    /**
     * Multiple other cancers indicator.
     */
    public final String multipleOtherCancers;
    /**
     * Multiple ovarian cancers indicator.
     */
    public final String multipleOvarianCancers;
    /**
     * Multiple pancreatic cancers indicator.
     */
    public final String multiplePancreaticCancers;
    /**
     * Multiple prostate cancers indicator.
     */
    public final String multipleProstateCancers;
    /**
     * Multiple stomach cancers indicator.
     */
    public final String multipleStomachCancers;
    /**
     * Multiple testicular cancers indicator.
     */
    public final String multipleTesticularCancers;
    /**
     * Multiple thyroid cancers indicator.
     */
    public final String multipleThyroidCancers;

    /**
     * Creates a new FamilyHistoryRecord with all demographic and cancer data.
     *
     * @param step the survey step/relationship type
     * @param stepInstance optional step instance identifier
     * @param relationship the relationship type
     * @param age age of the family member
     * @param gender gender of the family member
     * @param vitalStatus vital status (alive/deceased)
     * @param sharedParent shared parent indicator for step-siblings
     * @param ashkenazi Ashkenazi descent indicator
     * @param bladderCancer bladder cancer diagnosis
     * @param bladderCancerAge age at bladder cancer diagnosis
     * @param breastCancer breast cancer diagnosis
     * @param breastCancerAge age at breast cancer diagnosis
     * @param tripleNegativeBreastCancer triple negative breast cancer diagnosis
     * @param colonOrRectalCancer colon/rectal cancer diagnosis
     * @param colonOrRectalCancerAge age at colon/rectal cancer diagnosis
     * @param endometrialOrUterineCancer endometrial/uterine cancer diagnosis
     * @param endometrialOrUterineCancerAge age at endometrial/uterine cancer diagnosis
     * @param kidneyRenalCellCancer kidney/renal cell cancer diagnosis
     * @param kidneyRenalCellCancerAge age at kidney/renal cell cancer diagnosis
     * @param leukemia leukemia diagnosis
     * @param leukemiaAge age at leukemia diagnosis
     * @param lungCancer lung cancer diagnosis
     * @param lungCancerAge age at lung cancer diagnosis
     * @param lymphoma lymphoma diagnosis
     * @param lymphomaAge age at lymphoma diagnosis
     * @param melanomaSkinCancer melanoma/skin cancer diagnosis
     * @param melanomaSkinCancerAge age at melanoma/skin cancer diagnosis
     * @param nonmelanomaSkinCancer non-melanoma skin cancer diagnosis
     * @param nonmelanomaSkinCancerAge age at non-melanoma skin cancer diagnosis
     * @param oralCavityOrThroatCancer oral cavity/throat cancer diagnosis
     * @param oralCavityOrThroatCancerAge age at oral cavity/throat cancer diagnosis
     * @param otherCancer other cancer diagnosis
     * @param otherAge age at other cancer diagnosis
     * @param otherCancerName name of other cancer type
     * @param ovarianCancer ovarian cancer diagnosis
     * @param ovarianCancerAge age at ovarian cancer diagnosis
     * @param pancreaticCancer pancreatic cancer diagnosis
     * @param pancreaticCancerAge age at pancreatic cancer diagnosis
     * @param prostateCancer prostate cancer diagnosis
     * @param prostateCancerAge age at prostate cancer diagnosis
     * @param stomachCancer stomach cancer diagnosis
     * @param stomachCancerAge age at stomach cancer diagnosis
     * @param testicularCancer testicular cancer diagnosis
     * @param testicularCancerAge age at testicular cancer diagnosis
     * @param thyroidCancer thyroid cancer diagnosis
     * @param thyroidCancerAge age at thyroid cancer diagnosis
     * @param unknownCancer unknown cancer diagnosis
     * @param unknownCancerAge age at unknown cancer diagnosis
     * @param multipleBladdercancers multiple bladder cancers indicator
     * @param multipleBreastcancers multiple breast cancers indicator
     * @param multipleColonOrRectalCancers multiple colon/rectal cancers indicator
     * @param multipleEndometrialOrUterineCancers multiple endometrial/uterine cancers indicator
     * @param multipleKidneyRenalCellCancers multiple kidney/renal cell cancers indicator
     * @param multipleLeukemias multiple leukemias indicator
     * @param multipleLungCancers multiple lung cancers indicator
     * @param multipleLymphomas multiple lymphomas indicator
     * @param multipleMelanomaSkinCancers multiple melanoma/skin cancers indicator
     * @param multipleNonmelanomaSkinCancers multiple non-melanoma skin cancers indicator
     * @param multipleOralCavityOrThroatCancers multiple oral cavity/throat cancers indicator
     * @param multipleOtherCancers multiple other cancers indicator
     * @param multipleOvarianCancers multiple ovarian cancers indicator
     * @param multiplePancreaticCancers multiple pancreatic cancers indicator
     * @param multipleProstateCancers multiple prostate cancers indicator
     * @param multipleStomachCancers multiple stomach cancers indicator
     * @param multipleTesticularCancers multiple testicular cancers indicator
     * @param multipleThyroidCancers multiple thyroid cancers indicator
     */
    public FamilyHistoryRecord(
            String step, String stepInstance, String relationship, Integer age, String gender,
            String vitalStatus, String sharedParent, String ashkenazi,
            String bladderCancer, Integer bladderCancerAge,
            String breastCancer, Integer breastCancerAge, String tripleNegativeBreastCancer,
            String colonOrRectalCancer, Integer colonOrRectalCancerAge,
            String endometrialOrUterineCancer, Integer endometrialOrUterineCancerAge,
            String kidneyRenalCellCancer, Integer kidneyRenalCellCancerAge,
            String leukemia, Integer leukemiaAge,
            String lungCancer, Integer lungCancerAge,
            String lymphoma, Integer lymphomaAge,
            String melanomaSkinCancer, Integer melanomaSkinCancerAge,
            String nonmelanomaSkinCancer, Integer nonmelanomaSkinCancerAge,
            String oralCavityOrThroatCancer, Integer oralCavityOrThroatCancerAge,
            String otherCancer, Integer otherAge, String otherCancerName,
            String ovarianCancer, Integer ovarianCancerAge,
            String pancreaticCancer, Integer pancreaticCancerAge,
            String prostateCancer, Integer prostateCancerAge,
            String stomachCancer, Integer stomachCancerAge,
            String testicularCancer, Integer testicularCancerAge,
            String thyroidCancer, Integer thyroidCancerAge,
            String unknownCancer, Integer unknownCancerAge,
            String multipleBladdercancers, String multipleBreastcancers,
            String multipleColonOrRectalCancers, String multipleEndometrialOrUterineCancers,
            String multipleKidneyRenalCellCancers, String multipleLeukemias,
            String multipleLungCancers, String multipleLymphomas,
            String multipleMelanomaSkinCancers, String multipleNonmelanomaSkinCancers,
            String multipleOralCavityOrThroatCancers, String multipleOtherCancers,
            String multipleOvarianCancers, String multiplePancreaticCancers,
            String multipleProstateCancers, String multipleStomachCancers,
            String multipleTesticularCancers, String multipleThyroidCancers
    ) {
        this.step = step;
        this.stepInstance = stepInstance;
        this.relationship = relationship;
        this.age = age;
        this.gender = gender;
        this.vitalStatus = vitalStatus;
        this.sharedParent = sharedParent;
        this.ashkenazi = ashkenazi;
        this.bladderCancer = bladderCancer;
        this.bladderCancerAge = bladderCancerAge;
        this.breastCancer = breastCancer;
        this.breastCancerAge = breastCancerAge;
        this.tripleNegativeBreastCancer = tripleNegativeBreastCancer;
        this.colonOrRectalCancer = colonOrRectalCancer;
        this.colonOrRectalCancerAge = colonOrRectalCancerAge;
        this.endometrialOrUterineCancer = endometrialOrUterineCancer;
        this.endometrialOrUterineCancerAge = endometrialOrUterineCancerAge;
        this.kidneyRenalCellCancer = kidneyRenalCellCancer;
        this.kidneyRenalCellCancerAge = kidneyRenalCellCancerAge;
        this.leukemia = leukemia;
        this.leukemiaAge = leukemiaAge;
        this.lungCancer = lungCancer;
        this.lungCancerAge = lungCancerAge;
        this.lymphoma = lymphoma;
        this.lymphomaAge = lymphomaAge;
        this.melanomaSkinCancer = melanomaSkinCancer;
        this.melanomaSkinCancerAge = melanomaSkinCancerAge;
        this.nonmelanomaSkinCancer = nonmelanomaSkinCancer;
        this.nonmelanomaSkinCancerAge = nonmelanomaSkinCancerAge;
        this.oralCavityOrThroatCancer = oralCavityOrThroatCancer;
        this.oralCavityOrThroatCancerAge = oralCavityOrThroatCancerAge;
        this.otherCancer = otherCancer;
        this.otherAge = otherAge;
        this.otherCancerName = otherCancerName;
        this.ovarianCancer = ovarianCancer;
        this.ovarianCancerAge = ovarianCancerAge;
        this.pancreaticCancer = pancreaticCancer;
        this.pancreaticCancerAge = pancreaticCancerAge;
        this.prostateCancer = prostateCancer;
        this.prostateCancerAge = prostateCancerAge;
        this.stomachCancer = stomachCancer;
        this.stomachCancerAge = stomachCancerAge;
        this.testicularCancer = testicularCancer;
        this.testicularCancerAge = testicularCancerAge;
        this.thyroidCancer = thyroidCancer;
        this.thyroidCancerAge = thyroidCancerAge;
        this.unknownCancer = unknownCancer;
        this.unknownCancerAge = unknownCancerAge;
        this.multipleBladdercancers = multipleBladdercancers;
        this.multipleBreastcancers = multipleBreastcancers;
        this.multipleColonOrRectalCancers = multipleColonOrRectalCancers;
        this.multipleEndometrialOrUterineCancers = multipleEndometrialOrUterineCancers;
        this.multipleKidneyRenalCellCancers = multipleKidneyRenalCellCancers;
        this.multipleLeukemias = multipleLeukemias;
        this.multipleLungCancers = multipleLungCancers;
        this.multipleLymphomas = multipleLymphomas;
        this.multipleMelanomaSkinCancers = multipleMelanomaSkinCancers;
        this.multipleNonmelanomaSkinCancers = multipleNonmelanomaSkinCancers;
        this.multipleOralCavityOrThroatCancers = multipleOralCavityOrThroatCancers;
        this.multipleOtherCancers = multipleOtherCancers;
        this.multipleOvarianCancers = multipleOvarianCancers;
        this.multiplePancreaticCancers = multiplePancreaticCancers;
        this.multipleProstateCancers = multipleProstateCancers;
        this.multipleStomachCancers = multipleStomachCancers;
        this.multipleTesticularCancers = multipleTesticularCancers;
        this.multipleThyroidCancers = multipleThyroidCancers;
    }
}
