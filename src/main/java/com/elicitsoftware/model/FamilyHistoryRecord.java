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
    public final String step;
    public final String stepInstance;
    public final String relationship;
    public final Integer age;
    public final String gender;
    public final String vitalStatus;
    public final String sharedParent;
    public final String ashkenazi;

    // Cancer history fields (56 total cancer-related columns)
    public final String bladderCancer;
    public final Integer bladderCancerAge;
    public final String breastCancer;
    public final Integer breastCancerAge;
    public final String tripleNegativeBreastCancer;
    public final String colonOrRectalCancer;
    public final Integer colonOrRectalCancerAge;
    public final String endometrialOrUterineCancer;
    public final Integer endometrialOrUterineCancerAge;
    public final String kidneyRenalCellCancer;
    public final Integer kidneyRenalCellCancerAge;
    public final String leukemia;
    public final Integer leukemiaAge;
    public final String lungCancer;
    public final Integer lungCancerAge;
    public final String lymphoma;
    public final Integer lymphomaAge;
    public final String melanomaSkinCancer;
    public final Integer melanomaSkinCancerAge;
    public final String nonmelanomaSkinCancer;
    public final Integer nonmelanomaSkinCancerAge;
    public final String oralCavityOrThroatCancer;
    public final Integer oralCavityOrThroatCancerAge;
    public final String otherCancer;
    public final Integer otherAge;
    public final String otherCancerName;
    public final String ovarianCancer;
    public final Integer ovarianCancerAge;
    public final String pancreaticCancer;
    public final Integer pancreaticCancerAge;
    public final String prostateCancer;
    public final Integer prostateCancerAge;
    public final String stomachCancer;
    public final Integer stomachCancerAge;
    public final String testicularCancer;
    public final Integer testicularCancerAge;
    public final String thyroidCancer;
    public final Integer thyroidCancerAge;
    public final String unknownCancer;
    public final Integer unknownCancerAge;
    public final String multipleBladdercancers;
    public final String multipleBreastcancers;
    public final String multipleColonOrRectalCancers;
    public final String multipleEndometrialOrUterineCancers;
    public final String multipleKidneyRenalCellCancers;
    public final String multipleLeukemias;
    public final String multipleLungCancers;
    public final String multipleLymphomas;
    public final String multipleMelanomaSkinCancers;
    public final String multipleNonmelanomaSkinCancers;
    public final String multipleOralCavityOrThroatCancers;
    public final String multipleOtherCancers;
    public final String multipleOvarianCancers;
    public final String multiplePancreaticCancers;
    public final String multipleProstateCancers;
    public final String multipleStomachCancers;
    public final String multipleTesticularCancers;
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
