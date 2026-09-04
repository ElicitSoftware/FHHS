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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression/characterization tests for {@link FamilyManager}.
 * <p>
 * FamilyManager builds the entire pedigree tree by matching {@code FamilyHistoryRecord.step}
 * against hardcoded string literals ("Mother", "Sibling", "Mother's Sibling", ...). These
 * literals originate from {@code dim_step.value} via {@code fact_sections_view}. Under
 * Survey's planned Kimball Type 2 migration, {@code dim_step.value} is updated in place
 * (SCD Type 1) whenever a step is renamed in the Author Tool - see
 * {@code research/Kimball_type2.md} section 3. These tests lock down the current behavior
 * so that any refactor (or any future step rename) that changes this matching is caught
 * immediately instead of silently dropping family members from reports.
 */
class FamilyManagerTest {

    private FamilyManager newManager() {
        FamilyManager manager = new FamilyManager();
        manager.cancerHistoryRepository = Mockito.mock(CancerHistoryRepository.class);
        return manager;
    }

    // ------------------------------------------------------------------
    // Builder for the 66-arg FamilyHistoryRecord constructor
    // ------------------------------------------------------------------

    private static final class Rec {
        String step;
        String stepInstance;
        String relationship;
        Integer age;
        String gender;
        String vitalStatus;
        String sharedParent;
        String ashkenazi;
        String bladderCancer;
        Integer bladderCancerAge;
        String breastCancer;
        Integer breastCancerAge;
        String tripleNegativeBreastCancer;
        String colonOrRectalCancer;
        Integer colonOrRectalCancerAge;
        String endometrialOrUterineCancer;
        Integer endometrialOrUterineCancerAge;
        String kidneyRenalCellCancer;
        Integer kidneyRenalCellCancerAge;
        String leukemia;
        Integer leukemiaAge;
        String lungCancer;
        Integer lungCancerAge;
        String lymphoma;
        Integer lymphomaAge;
        String melanomaSkinCancer;
        Integer melanomaSkinCancerAge;
        String nonmelanomaSkinCancer;
        Integer nonmelanomaSkinCancerAge;
        String oralCavityOrThroatCancer;
        Integer oralCavityOrThroatCancerAge;
        String otherCancer;
        Integer otherAge;
        String otherCancerName;
        String ovarianCancer;
        Integer ovarianCancerAge;
        String pancreaticCancer;
        Integer pancreaticCancerAge;
        String prostateCancer;
        Integer prostateCancerAge;
        String stomachCancer;
        Integer stomachCancerAge;
        String testicularCancer;
        Integer testicularCancerAge;
        String thyroidCancer;
        Integer thyroidCancerAge;
        String unknownCancer;
        Integer unknownCancerAge;
        String multipleBladdercancers;
        String multipleBreastcancers;
        String multipleColonOrRectalCancers;
        String multipleEndometrialOrUterineCancers;
        String multipleKidneyRenalCellCancers;
        String multipleLeukemias;
        String multipleLungCancers;
        String multipleLymphomas;
        String multipleMelanomaSkinCancers;
        String multipleNonmelanomaSkinCancers;
        String multipleOralCavityOrThroatCancers;
        String multipleOtherCancers;
        String multipleOvarianCancers;
        String multiplePancreaticCancers;
        String multipleProstateCancers;
        String multipleStomachCancers;
        String multipleTesticularCancers;
        String multipleThyroidCancers;

        Rec step(String v) { this.step = v; return this; }
        Rec age(Integer v) { this.age = v; return this; }
        Rec gender(String v) { this.gender = v; return this; }
        Rec vitalStatus(String v) { this.vitalStatus = v; return this; }
        Rec sharedParent(String v) { this.sharedParent = v; return this; }
        Rec ashkenazi(String v) { this.ashkenazi = v; return this; }
        Rec stepInstance(String v) { this.stepInstance = v; return this; }
        Rec breastCancer(String v, Integer a, String multiple) {
            this.breastCancer = v; this.breastCancerAge = a; this.multipleBreastcancers = multiple; return this;
        }
        Rec otherCancer(String v, Integer a, String name, String multiple) {
            this.otherCancer = v; this.otherAge = a; this.otherCancerName = name; this.multipleOtherCancers = multiple; return this;
        }

        FamilyHistoryRecord build() {
            return new FamilyHistoryRecord(
                    step, stepInstance, relationship, age, gender,
                    vitalStatus, sharedParent, ashkenazi,
                    bladderCancer, bladderCancerAge,
                    breastCancer, breastCancerAge, tripleNegativeBreastCancer,
                    colonOrRectalCancer, colonOrRectalCancerAge,
                    endometrialOrUterineCancer, endometrialOrUterineCancerAge,
                    kidneyRenalCellCancer, kidneyRenalCellCancerAge,
                    leukemia, leukemiaAge,
                    lungCancer, lungCancerAge,
                    lymphoma, lymphomaAge,
                    melanomaSkinCancer, melanomaSkinCancerAge,
                    nonmelanomaSkinCancer, nonmelanomaSkinCancerAge,
                    oralCavityOrThroatCancer, oralCavityOrThroatCancerAge,
                    otherCancer, otherAge, otherCancerName,
                    ovarianCancer, ovarianCancerAge,
                    pancreaticCancer, pancreaticCancerAge,
                    prostateCancer, prostateCancerAge,
                    stomachCancer, stomachCancerAge,
                    testicularCancer, testicularCancerAge,
                    thyroidCancer, thyroidCancerAge,
                    unknownCancer, unknownCancerAge,
                    multipleBladdercancers, multipleBreastcancers,
                    multipleColonOrRectalCancers, multipleEndometrialOrUterineCancers,
                    multipleKidneyRenalCellCancers, multipleLeukemias,
                    multipleLungCancers, multipleLymphomas,
                    multipleMelanomaSkinCancers, multipleNonmelanomaSkinCancers,
                    multipleOralCavityOrThroatCancers, multipleOtherCancers,
                    multipleOvarianCancers, multiplePancreaticCancers,
                    multipleProstateCancers, multipleStomachCancers,
                    multipleTesticularCancers, multipleThyroidCancers
            );
        }
    }

    private static Rec rec(String step, String gender, Integer age, String vitalStatus) {
        return new Rec().step(step).gender(gender).age(age).vitalStatus(vitalStatus);
    }

    private FamilyMember findByName(Family family, String name) {
        return family.family.stream().filter(m -> name.equals(m.name)).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------
    // Basic proband-only family
    // ------------------------------------------------------------------

    @Test
    void probandOnly_getsIdSevenAndRespondentName() {
        FamilyManager manager = newManager();
        manager.addFamily(List.of(rec("Proband", "Female", 30, "alive").build()));

        Family family = manager.getFamily();

        // A lone proband with no reported parents still yields 3 members: addMissingParents()
        // unconditionally backfills an "unknown" Father and Mother placeholder so the pedigree
        // tool always has two parent nodes to draw.
        assertEquals(3, family.family.size());
        FamilyMember proband = findByName(family, "Respondent");
        assertNotNull(proband);
        assertEquals(7, proband.ID);
        assertEquals(2, proband.Sex);

        FamilyMember father = findByName(family, "Father");
        FamilyMember mother = findByName(family, "Mother");
        assertNotNull(father);
        assertNotNull(mother);
        assertTrue(father.unknown);
        assertTrue(mother.unknown);
    }

    @Test
    void demographicsAndProbandCancerStepsAllMapToSameProbandPerson() {
        // "Proband", "Proband Cancer" and "Demographics" are three distinct step values
        // that must all resolve to the SAME Person - this is exactly the kind of
        // multi-literal matching that a step rename could desynchronize.
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Demographics", "Female", 30, "alive").build(),
                rec("Proband", "Female", 30, "alive").build(),
                rec("Proband Cancer", "Female", 30, "alive").build()
        ));

        Family family = manager.getFamily();

        long respondentCount = family.family.stream().filter(m -> "Respondent".equals(m.name)).count();
        assertEquals(1, respondentCount, "all three proband-step records must collapse into one person");
    }

    // ------------------------------------------------------------------
    // Unknown / renamed step names are silently dropped (documents the Kimball risk)
    // ------------------------------------------------------------------

    @Test
    void unrecognizedStepIsSilentlyDroppedNotThrown() {
        // FamilyManager.getPerson() throws RelationException for any step string it doesn't
        // recognize, and addFamily() swallows that exception. If a Kimball Type 2 SCD1 rename
        // changes "Sibling" to e.g. "Brother or Sister" without updating this switch statement,
        // that family member silently disappears from the report with no error and no record.
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Proband", "Female", 30, "alive").build(),
                rec("Brother or Sister", "Male", 25, "alive").build() // renamed "Sibling"
        ));

        Family family = manager.getFamily();

        // Same baseline as the lone-proband case (proband + 2 unknown-parent placeholders) -
        // the unrecognized step contributes nothing, silently.
        assertEquals(3, family.family.size(), "unrecognized step must be dropped, not added or thrown");
        assertNull(findByName(family, "Sibling_1"));
    }

    // ------------------------------------------------------------------
    // Core relationships and pedigree ID assignment (setIDs)
    // ------------------------------------------------------------------

    @Test
    void motherFatherAndProband_getStandardPedigreeIds() {
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Proband", "Male", 40, "alive").build(),
                rec("Mother", "Female", 65, "alive").build(),
                rec("Father", "Male", 68, "alive").build()
        ));

        Family family = manager.getFamily();

        FamilyMember proband = findByName(family, "Respondent");
        FamilyMember mother = findByName(family, "Mother");
        FamilyMember father = findByName(family, "Father");

        assertEquals(7, proband.ID);
        assertEquals(6, mother.ID);
        assertEquals(5, father.ID);
        assertEquals(5, proband.Dadid);
        assertEquals(6, proband.Momid);
    }

    @Test
    void grandparents_areLinkedToParentsById() {
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Proband", "Male", 40, "alive").build(),
                rec("Father", "Male", 68, "alive").build(),
                rec("Paternal Grandmother", "Female", 90, "deceased").build(),
                rec("Paternal Grandfather", "Male", 92, "deceased").build()
        ));

        Family family = manager.getFamily();
        FamilyMember father = findByName(family, "Father");
        FamilyMember pgf = findByName(family, "Grandfather");
        FamilyMember pgm = findByName(family, "Grandmother");

        assertEquals(1, pgf.ID);
        assertEquals(2, pgm.ID);
        assertEquals(1, father.Dadid);
        assertEquals(2, father.Momid);
        assertTrue(pgm.Status == 1, "deceased vital status must map to Status=1");
    }

    @Test
    void missingFather_isBackfilledAsUnknownWithId5() {
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Proband", "Male", 40, "alive").build(),
                rec("Mother", "Female", 65, "alive").build()
        ));

        Family family = manager.getFamily();
        FamilyMember proband = findByName(family, "Respondent");

        assertEquals(5, proband.Dadid, "an unknown father placeholder must still get ID 5");
        boolean hasUnknownFather = family.family.stream().anyMatch(m -> m.unknown && m.ID == 5);
        assertTrue(hasUnknownFather);
    }

    @Test
    void childOfMaleProband_getsUnknownWifeAsOtherParent() {
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Proband", "Male", 40, "alive").build(),
                rec("Child", "Female", 10, "alive").build()
        ));

        Family family = manager.getFamily();
        FamilyMember child = findByName(family, "Child_1");
        FamilyMember unknownWife = findByName(family, "Unknown");

        assertNotNull(unknownWife);
        assertEquals(-4, unknownWife.ID);
        assertEquals(7, child.Dadid);
        assertEquals(-4, child.Momid);
    }

    @Test
    void childOfFemaleProband_getsUnknownHusbandAsOtherParent() {
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Proband", "Female", 40, "alive").build(),
                rec("Child", "Male", 10, "alive").build()
        ));

        Family family = manager.getFamily();
        FamilyMember child = findByName(family, "Child_1");

        assertEquals(7, child.Momid);
        assertEquals(-3, child.Dadid);
    }

    @Test
    void probandGenderOther_withChildren_isRemodeledAsFemale() {
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Proband", "Other", 40, "alive").build(),
                rec("Child", "Male", 10, "alive").build()
        ));

        Family family = manager.getFamily();
        FamilyMember proband = findByName(family, "Respondent");

        assertEquals(2, proband.Sex, "Other+children proband must be remapped to female (Sex=2) for Kinship2");
    }

    @Test
    void siblingSharedParentFather_getsUnknownMotherAsOtherParent() {
        FamilyManager manager = newManager();
        Rec halfSibling = rec("Sibling", "Male", 20, "alive").sharedParent("father");
        manager.addFamily(List.of(
                rec("Proband", "Female", 40, "alive").build(),
                halfSibling.build()
        ));

        Family family = manager.getFamily();
        FamilyMember sibling = findByName(family, "Sibling_1");
        FamilyMember unknownMother = findByName(family, "Unknown_Mother");

        assertNotNull(unknownMother);
        assertEquals(-2, unknownMother.ID);
        assertEquals(5, sibling.Dadid);
        assertEquals(-2, sibling.Momid);
    }

    @Test
    void siblingSharedParentMother_getsUnknownFatherAsOtherParent() {
        FamilyManager manager = newManager();
        Rec halfSibling = rec("Sibling", "Male", 20, "alive").sharedParent("mother");
        manager.addFamily(List.of(
                rec("Proband", "Female", 40, "alive").build(),
                halfSibling.build()
        ));

        Family family = manager.getFamily();
        FamilyMember sibling = findByName(family, "Sibling_1");

        assertEquals(6, sibling.Momid);
        assertEquals(-1, sibling.Dadid);
    }

    @Test
    void fullSibling_noSharedParent_usesBothStandardParents() {
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Proband", "Female", 40, "alive").build(),
                rec("Sibling", "Male", 20, "alive").build()
        ));

        Family family = manager.getFamily();
        FamilyMember sibling = findByName(family, "Sibling_1");

        assertEquals(5, sibling.Dadid);
        assertEquals(6, sibling.Momid);
    }

    @Test
    void mothersSiblingAndFathersSibling_areLinkedToCorrectGrandparentPair() {
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Proband", "Female", 40, "alive").build(),
                rec("Mother's Sibling", "Female", 55, "alive").build(),
                rec("Father's Sibling", "Male", 58, "alive").build()
        ));

        Family family = manager.getFamily();
        FamilyMember aunt = findByName(family, "Aunt_1");
        FamilyMember uncle = findByName(family, "Uncle_1");

        assertNotNull(aunt, "Mother's Sibling with Sex=2 should render as Aunt_1");
        assertEquals(3, aunt.Dadid);
        assertEquals(4, aunt.Momid);

        assertNotNull(uncle, "Father's Sibling with Sex=1 should render as Uncle_1");
        assertEquals(1, uncle.Dadid);
        assertEquals(2, uncle.Momid);
    }

    @Test
    void multipleChildrenAndSiblings_eachGetDistinctSequentialIds() {
        FamilyManager manager = newManager();
        manager.addFamily(List.of(
                rec("Proband", "Female", 40, "alive").build(),
                new Rec().step("Child").stepInstance("1").gender("Male").age(10).vitalStatus("alive").build(),
                new Rec().step("Child").stepInstance("2").gender("Female").age(8).vitalStatus("alive").build()
        ));

        Family family = manager.getFamily();
        FamilyMember child1 = findByName(family, "Child_1");
        FamilyMember child2 = findByName(family, "Child_2");

        assertNotNull(child1);
        assertNotNull(child2);
        assertNotEquals(child1.ID, child2.ID);
    }

    // ------------------------------------------------------------------
    // Cancer history mapping (hasCancerHistory + field propagation)
    // ------------------------------------------------------------------

    @Test
    void breastCancerTrue_propagatesAgeAndMultipleFlagToFamilyMember() {
        FamilyManager manager = newManager();
        Rec proband = rec("Proband", "Female", 50, "alive").breastCancer("true", 45, "true");
        manager.addFamily(List.of(proband.build()));

        FamilyMember member = findByName(manager.getFamily(), "Respondent");

        assertEquals("45", member.Breast_Cancer);
        assertEquals("true", member.Multiple_Breast_Cancer);
        assertTrue(member.hasMultipleCancers());
    }

    @Test
    void breastCancerTrue_withNullAge_usesUnknownAgePlaceholder() {
        FamilyManager manager = newManager();
        Rec proband = rec("Proband", "Female", 50, "alive").breastCancer("true", null, null);
        manager.addFamily(List.of(proband.build()));

        FamilyMember member = findByName(manager.getFamily(), "Respondent");

        assertEquals("unk. age", member.Breast_Cancer);
    }

    @Test
    void breastCancerFalse_doesNotSetCancerField() {
        FamilyManager manager = newManager();
        Rec proband = rec("Proband", "Female", 50, "alive").breastCancer("false", 45, null);
        manager.addFamily(List.of(proband.build()));

        FamilyMember member = findByName(manager.getFamily(), "Respondent");

        assertNull(member.Breast_Cancer);
    }

    @Test
    void hasCancerHistory_acceptsUnknownYesYAnd1AsPositive() {
        for (String truthy : new String[]{"true", "TRUE", "unknown", "yes", "y", "1", " true "}) {
            FamilyManager manager = newManager();
            Rec proband = rec("Proband", "Female", 50, "alive").breastCancer(truthy, 45, null);
            manager.addFamily(List.of(proband.build()));

            FamilyMember member = findByName(manager.getFamily(), "Respondent");
            assertNotNull(member.Breast_Cancer, "value '" + truthy + "' should be treated as cancer-positive");
        }
    }

    @Test
    void hasCancerHistory_rejectsNoAndEmptyAndNull() {
        for (String falsy : new String[]{"no", "", null, "2"}) {
            FamilyManager manager = newManager();
            Rec proband = rec("Proband", "Female", 50, "alive").breastCancer(falsy, 45, null);
            manager.addFamily(List.of(proband.build()));

            FamilyMember member = findByName(manager.getFamily(), "Respondent");
            assertNull(member.Breast_Cancer, "value '" + falsy + "' should not be treated as cancer-positive");
        }
    }

    @Test
    void otherCancer_propagatesNameAndAge() {
        FamilyManager manager = newManager();
        Rec proband = rec("Proband", "Female", 50, "alive").otherCancer("true", 60, "Sarcoma", "false");
        manager.addFamily(List.of(proband.build()));

        FamilyMember member = findByName(manager.getFamily(), "Respondent");

        assertEquals("60", member.Other_Cancer);
        assertEquals("Sarcoma", member.Other_Cancer_Type);
    }

    @Test
    void ashkenazi_spacesAreReplacedWithUnderscoresOnFamilyMember() {
        FamilyManager manager = newManager();
        Rec proband = rec("Proband", "Female", 50, "alive").ashkenazi("Both Parents");
        manager.addFamily(List.of(proband.build()));

        FamilyMember member = findByName(manager.getFamily(), "Respondent");

        assertEquals("Both_Parents", member.Ashkenazi);
    }

    // ------------------------------------------------------------------
    // Repository wiring
    // ------------------------------------------------------------------

    @Test
    void getFamilyById_delegatesToInjectedRepository() {
        FamilyManager manager = newManager();
        List<FamilyHistoryRecord> stubbed = new ArrayList<>();
        stubbed.add(rec("Proband", "Female", 50, "alive").build());
        Mockito.when(manager.cancerHistoryRepository.findFamilyHistoryByRespondentId(42L)).thenReturn(stubbed);

        Family family = manager.getFamily(42L);

        assertNotNull(findByName(family, "Respondent"));
        Mockito.verify(manager.cancerHistoryRepository).findFamilyHistoryByRespondentId(42L);
    }

    @Test
    void findByRespondentid_delegatesToInjectedRepository() {
        FamilyManager manager = newManager();
        List<FamilyHistoryRecord> stubbed = List.of(rec("Proband", "Male", 50, "alive").build());
        Mockito.when(manager.cancerHistoryRepository.findFamilyHistoryByRespondentId(7L)).thenReturn(stubbed);

        List<FamilyHistoryRecord> result = manager.findByRespondentid(7L);

        assertSame(stubbed, result);
    }
}
