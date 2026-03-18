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

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.management.relation.RelationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Manages family structure and relationships for family history health surveys.
 * <p>
 * This class is responsible for building and organizing complete family structures
 * from survey data. It processes family member information including relationships,
 * cancer history, demographics, and vital status to create comprehensive family
 * trees used for pedigree generation and analysis.
 * </p>
 * <p>
 * The class supports various family relationship types including immediate family
 * (parents, siblings, children), extended family (aunts, uncles, grandparents),
 * and complex relationships (step-siblings, spouses).
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@RequestScoped
public class FamilyManager {

    private static final Logger LOG = Logger.getLogger(FamilyManager.class);

    /**
     * Repository for optimized family history queries.
     */
    @Inject
    CancerHistoryRepository cancerHistoryRepository;

    /**
     * Default constructor.
     */
    public FamilyManager() {
        // Default constructor for CDI
    }

    /**
     * Default value used when age is unknown.
     */
    private static final String UKN_AGE = "unk. age";

    /**
     * String constant representing boolean true in survey data.
     */
    private static final String STRING_TRUE = "true";
    private static final String STRING_UNKNOWN = "unknown";

    private static boolean hasCancerHistory(String response) {
        if (response == null) {
            return false;
        }

        String normalized = response.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return false;
        }

        return STRING_TRUE.equals(normalized)
                || STRING_UNKNOWN.equals(normalized)
                || "yes".equals(normalized)
                || "y".equals(normalized)
                || "1".equals(normalized);
    }

    /**
     * Gender constant for male family members.
     */
    private static final String MALE = "Male";

    /**
     * Gender constant for female family members.
     */
    private static final String FEMALE = "Female";

    // Family member collections for one-to-many relationships
    /**
     * Collection of the proband's children organized by identifier.
     */
    private final LinkedHashMap<String, Person> Children = new LinkedHashMap<>();

    /**
     * Collection of the proband's siblings organized by identifier.
     */
    private final LinkedHashMap<String, Person> Siblings = new LinkedHashMap<>();

    /**
     * Collection of maternal aunts and uncles organized by identifier.
     */
    private final LinkedHashMap<String, Person> MaternalAuntsUncles = new LinkedHashMap<>();

    /**
     * Collection of paternal aunts and uncles organized by identifier.
     */
    private final LinkedHashMap<String, Person> PaternalAuntsUncles = new LinkedHashMap<>();

    /**
     * The complete family structure containing all relationships and data.
     */
    Family family = null;

    // Single-instance family members
    /**
     * The primary study participant (proband).
     */
    private Person Proband = null;

    /**
     * The proband's mother.
     */
    private Person Mother = null;

    /**
     * The proband's father.
     */
    private Person Father = null;

    /**
     * The proband's maternal grandmother.
     */
    private Person MaternalGrandmother = null;

    /**
     * The proband's maternal grandfather.
     */
    private Person MaternalGrandfather = null;

    /**
     * The proband's paternal grandmother.
     */
    private Person PaternalGrandmother = null;

    /**
     * The proband's paternal grandfather.
     */
    private Person PaternalGrandfather = null;

    // Additional family members for complex relationships
    /**
     * Unknown mother figure for step-sibling relationships.
     */
    private Person UnknownMother = null;

    /**
     * Unknown father figure for step-sibling relationships.
     */
    private Person UnknownFather = null;

    /**
     * Unknown husband/spouse figure for relationship modeling.
     */
    private Person UnknownHusband = null;

    /**
     * Unknown wife/spouse figure for relationship modeling.
     */
    private Person UnknownWife = null;

    /**
     * Retrieves all family history data for a specific respondent.
     * <p>
     * Queries the database for all family member records associated with the
     * given respondent ID using optimized native SQL queries that avoid expensive
     * dimension table joins.
     * </p>
     *
     * @param respondentId the unique identifier of the survey respondent
     * @return a list of FamilyHistoryRecord objects containing family member data
     */
    public List<FamilyHistoryRecord> findByRespondentid(long respondentId) {
        return cancerHistoryRepository.findFamilyHistoryByRespondentId(respondentId);
    }

    /**
     * Builds and returns a complete Family object for the specified respondent.
     * <p>
     * This method coordinates the entire family-building process:
     * <ol>
     *   <li>Retrieves all family member data from the optimized repository</li>
     *   <li>Processes each family member's information and relationships</li>
     *   <li>Assembles the complete family structure</li>
     * </ol>
     *
     * @param id the respondent ID to build the family for
     * @return a complete Family object containing all family members and relationships
     */
    public Family getFamily(long id) {
        List<FamilyHistoryRecord> rows = findByRespondentid(id);
        addFamily(rows);

        return getFamily();
    }

    /**
     * Processes a list of family member data and builds the family structure.
     * <p>
     * This method iterates through each family member's survey data and:
     * <ul>
     *   <li>Creates Person objects with demographic information</li>
     *   <li>Sets cancer history and multiple cancer indicators</li>
     *   <li>Establishes family relationships</li>
     *   <li>Handles special relationship cases (step-siblings, etc.)</li>
     * </ul>
     *
     * @param rows list of FamilyHistoryRecord objects containing raw family member data
     */
    public void addFamily(List<FamilyHistoryRecord> rows) {
        for (FamilyHistoryRecord fact : rows) {
            try {
                Person p = getPerson(fact);
                p.setAge(fact.age);
                p.setAshkenazi(fact.ashkenazi);
                p.setGender(fact.gender);
                p.setSharedParent(fact.sharedParent);
                p.setVital_Status(fact.vitalStatus);

                if (hasCancerHistory(fact.bladderCancer)) {
                    p.setBladder_Cancer(Objects.requireNonNullElse(fact.bladderCancerAge, UKN_AGE).toString());
                    p.setMultiple_Bladder_Cancer(fact.multipleBladdercancers);
                }
                if (hasCancerHistory(fact.breastCancer)) {
                    p.setBreast_Cancer(Objects.requireNonNullElse(fact.breastCancerAge, UKN_AGE).toString());
                    p.setMultiple_Breast_Cancer(fact.multipleBreastcancers);
                }
                if (hasCancerHistory(fact.colonOrRectalCancer)) {
                    p.setColon_Rectal_Cancer(Objects.requireNonNullElse(fact.colonOrRectalCancerAge, UKN_AGE).toString());
                    p.setMultiple_Colon_Rectal_Cancer(fact.multipleColonOrRectalCancers);
                }
                if (hasCancerHistory(fact.endometrialOrUterineCancer)) {
                    p.setEndometrial_Uterine_Cancer(Objects.requireNonNullElse(fact.endometrialOrUterineCancerAge, UKN_AGE).toString());
                    p.setMultiple_Endometrial_Uterine_Cancer(fact.multipleEndometrialOrUterineCancers);
                }
                if (hasCancerHistory(fact.kidneyRenalCellCancer)) {
                    p.setKidney_Renal_Cell_Cancer(Objects.requireNonNullElse(fact.kidneyRenalCellCancerAge, UKN_AGE).toString());
                    p.setMultiple_Kidney_Renal_Cell_Cancer(fact.multipleKidneyRenalCellCancers);
                }
                if (hasCancerHistory(fact.leukemia)) {
                    p.setLeukemia_Cancer(Objects.requireNonNullElse(fact.leukemiaAge, UKN_AGE).toString());
                    p.setMultiple_Leukemia_Cancer(fact.multipleLeukemias);
                }
                if (hasCancerHistory(fact.lungCancer)) {
                    p.setLung_Cancer(Objects.requireNonNullElse(fact.lungCancerAge, UKN_AGE).toString());
                    p.setMultiple_Lung_Cancer(fact.multipleLungCancers);
                }
                if (hasCancerHistory(fact.lymphoma)) {
                    p.setLymphoma(Objects.requireNonNullElse(fact.lymphomaAge, UKN_AGE).toString());
                    p.setMultiple_Lymphoma(fact.multipleLymphomas);
                }
                if (hasCancerHistory(fact.melanomaSkinCancer)) {
                    p.setMelanoma_Cancer(Objects.requireNonNullElse(fact.melanomaSkinCancerAge, UKN_AGE).toString());
                    p.setMultiple_Melanoma_Cancer(fact.multipleMelanomaSkinCancers);
                }
                if (hasCancerHistory(fact.nonmelanomaSkinCancer)) {
                    p.setNon_Melanoma_Cancer(Objects.requireNonNullElse(fact.nonmelanomaSkinCancerAge, UKN_AGE).toString());
                    p.setMultiple_Non_Melanoma_Cancer(fact.multipleNonmelanomaSkinCancers);
                }
                if (hasCancerHistory(fact.oralCavityOrThroatCancer)) {
                    p.setOral_Throat_Cancer(Objects.requireNonNullElse(fact.oralCavityOrThroatCancerAge, UKN_AGE).toString());
                    p.setMultiple_Oral_Throat_Cancer(fact.multipleOralCavityOrThroatCancers);
                }
                if (hasCancerHistory(fact.otherCancer)) {
                    p.setOther_Cancer(Objects.requireNonNullElse(fact.otherAge, UKN_AGE).toString());
                    p.setOther_Cancer_Type(fact.otherCancerName);
                    p.setMultiple_Other_Cancer(fact.multipleOtherCancers);
                }
                if (hasCancerHistory(fact.ovarianCancer)) {
                    p.setOvarian_Cancer(Objects.requireNonNullElse(fact.ovarianCancerAge, UKN_AGE).toString());
                    p.setMultiple_Ovarian_Cancer(fact.multipleOvarianCancers);
                }
                if (hasCancerHistory(fact.pancreaticCancer)) {
                    p.setPancreatic_Cancer(Objects.requireNonNullElse(fact.pancreaticCancerAge, UKN_AGE).toString());
                    p.setMultiple_Pancreatic_Cancer(fact.multiplePancreaticCancers);
                }
                if (hasCancerHistory(fact.prostateCancer)) {
                    p.setProstate_Cancer(Objects.requireNonNullElse(fact.prostateCancerAge, UKN_AGE).toString());
                    p.setMultiple_Prostate_Cancer(fact.multipleProstateCancers);
                }
                if (hasCancerHistory(fact.stomachCancer)) {
                    p.setStomach_Cancer(Objects.requireNonNullElse(fact.stomachCancerAge, UKN_AGE).toString());
                    p.setMultiple_Stomach_Cancer(fact.multipleStomachCancers);
                }
                if (hasCancerHistory(fact.testicularCancer)) {
                    p.setTesticular_Cancer(Objects.requireNonNullElse(fact.testicularCancerAge, UKN_AGE).toString());
                    p.setMultiple_Testicular_Cancer(fact.multipleTesticularCancers);
                }
                if (hasCancerHistory(fact.thyroidCancer)) {
                    p.setThyroid_Cancer(Objects.requireNonNullElse(fact.thyroidCancerAge, UKN_AGE).toString());
                    p.setMultiple_Thyroid_Cancer(fact.multipleThyroidCancers);
                }
                if (hasCancerHistory(fact.unknownCancer)) {
                    p.setUnknown_Cancer(Objects.requireNonNullElse(fact.unknownCancerAge, UKN_AGE).toString());
                }

                if ("Proband".equals(fact.step) || "Proband Cancer".equals(fact.step)) {
                    LOG.infof(
                            "Proband cancer mapping: step=%s breast=%s age=%s uterine=%s age=%s familyMember=%s",
                            fact.step,
                            fact.breastCancer,
                            fact.breastCancerAge,
                            fact.endometrialOrUterineCancer,
                            fact.endometrialOrUterineCancerAge,
                            p.getFamilyMember()
                    );
                }
            } catch (RelationException e) {
                // There are some steps that are not relationships. i.e. Demographics
            }
        }
    }

    /**
     * Gets or creates a Person object based on the relationship type in the fact record.
     * Uses lazy initialization to create Person objects only when needed.
     *
     * @param fact the FamilyHistoryRecord containing relationship information
     * @return the Person object corresponding to the relationship
     * @throws RelationException if the relationship type is not recognized
     */
    private Person getPerson(FamilyHistoryRecord fact) throws RelationException {
        String key = fact.step + Objects.requireNonNullElse(fact.stepInstance, "");
        switch (fact.step) {
            case "Proband":
            case "Proband Cancer":
            case "Demographics":
                if (this.Proband == null) {
                    this.Proband = new Person();
                }
                return this.Proband;
            case "Mother":
                if (this.Mother == null) {
                    this.Mother = new Person();
                }
                return this.Mother;
            case "Father":
                if (this.Father == null) {
                    this.Father = new Person();
                }
                return this.Father;
            case "Maternal Grandmother":
                if (this.MaternalGrandmother == null) {
                    this.MaternalGrandmother = new Person();
                }
                return this.MaternalGrandmother;
            case "Maternal Grandfather":
                if (this.MaternalGrandfather == null) {
                    this.MaternalGrandfather = new Person();
                }
                return this.MaternalGrandfather;
            case "Paternal Grandmother":
                if (this.PaternalGrandmother == null) {
                    this.PaternalGrandmother = new Person();
                }
                return this.PaternalGrandmother;
            case "Paternal Grandfather":
                if (this.PaternalGrandfather == null) {
                    this.PaternalGrandfather = new Person();
                }
                return this.PaternalGrandfather;
            case "Child":
                if (!this.Children.containsKey(key)) {
                    this.Children.put(key, new Person());
                }
                return this.Children.get(key);
            case "Sibling":
                if (!this.Siblings.containsKey(key)) {
                    this.Siblings.put(key, new Person());
                }
                return this.Siblings.get(key);
            case "Mother's Sibling":
                if (!this.MaternalAuntsUncles.containsKey(key)) {
                    this.MaternalAuntsUncles.put(key, new Person());
                }
                return this.MaternalAuntsUncles.get(key);
            case "Father's Sibling":
                if (!this.PaternalAuntsUncles.containsKey(key)) {
                    this.PaternalAuntsUncles.put(key, new Person());
                }
                return this.PaternalAuntsUncles.get(key);
            default:
                throw new RelationException(key + " is not a valid relationship");
        }

    }

    /**
     * Assigns unique IDs to all family members following pedigree conventions.
     * Sets parent relationship IDs (Dadid, Momid) to establish family structure.
     * IDs follow a specific numbering scheme for pedigree generation tools.
     */
    private void setIDs() {
        // Person ID F M

        // UnknownFather -1 0 0
        // UnknownMother -2 0 0
        // Paternal Grandfather 1 0 0
        // Paternal Grandmother 2 0 0
        // Maternal Grandfather 3 0 0
        // Maternal Grandmother 4 0 0
        // Father 5 1 2
        // Mother 6 3 4
        // Proband 7 5 6
        // Sibling ? 5 6
        // Child ? ?/8 ?/8

        int i = 8;

        this.Proband.getFamilyMember().ID = 7;

        setProbandSexIfOtherAndParent();

        // if the proband is adopted remove older generations
        // else set the parental ids.

        if (this.PaternalGrandfather != null) {
            this.PaternalGrandfather.getFamilyMember().ID = 1;
        }

        if (this.PaternalGrandmother != null) {
            this.PaternalGrandmother.getFamilyMember().ID = 2;
        }

        if (this.MaternalGrandfather != null) {
            this.MaternalGrandfather.getFamilyMember().ID = 3;
        }

        if (this.MaternalGrandmother != null) {
            this.MaternalGrandmother.getFamilyMember().ID = 4;
        }

        if (this.Father != null) {
            this.Father.getFamilyMember().ID = 5;
        }

        if (this.Mother != null) {
            this.Mother.getFamilyMember().ID = 6;
        }

        // if we have any missing parents or grand parents lets fill them
        // in with unknowns.
        addMissingParents();

        if (this.Father != null && this.PaternalGrandfather != null) {
            this.Father.getFamilyMember().Dadid = 1;
        }
        if (this.Father != null && this.PaternalGrandmother != null) {
            this.Father.getFamilyMember().Momid = 2;
        }
        if (this.Mother != null && this.MaternalGrandfather != null) {
            this.Mother.getFamilyMember().Dadid = 3;
        }
        if (this.Mother != null && this.MaternalGrandmother != null) {
            this.Mother.getFamilyMember().Momid = 4;
        }

        if (this.Proband != null) {
            if (this.Father != null) {
                this.Proband.getFamilyMember().Dadid = 5;
            }
            if (this.Mother != null) {
                this.Proband.getFamilyMember().Momid = 6;
            }
        }

        // Children
        for (Entry<String, Person> child : this.Children.entrySet()) {
            child.getValue().getFamilyMember().ID = i++;
            if (MALE.equalsIgnoreCase(this.Proband.getGender())) {
                child.getValue().getFamilyMember().Dadid = 7;
                addUnknownWife();
                child.getValue().getFamilyMember().Momid = this.UnknownWife.getFamilyMember().ID;
            } else {
                child.getValue().getFamilyMember().Momid = 7;
                addUnknownHusband();
                child.getValue().getFamilyMember().Dadid = this.UnknownHusband.getFamilyMember().ID;
            }
        }
        // Siblings
        for (Entry<String, Person> sibling : this.Siblings.entrySet()) {
            sibling.getValue().getFamilyMember().ID = i++;
            if (sibling.getValue().getSharedParent() != null) {
                if (sibling.getValue().getSharedParent().equals("father")) {
                    sibling.getValue().getFamilyMember().Dadid = 5;
                    addUnknownMother();
                    sibling.getValue().getFamilyMember().Momid = this.UnknownMother.getFamilyMember().ID;
                } else if (sibling.getValue().getSharedParent().equals("mother")) {
                    sibling.getValue().getFamilyMember().Momid = 6;
                    addUnknownFather();
                    sibling.getValue().getFamilyMember().Dadid = this.UnknownFather.getFamilyMember().ID;
                }
            } else {
                sibling.getValue().getFamilyMember().Dadid = 5;
                sibling.getValue().getFamilyMember().Momid = 6;
            }
        }

        for (Entry<String, Person> auntUncle : this.MaternalAuntsUncles.entrySet()) {
            auntUncle.getValue().getFamilyMember().ID = i++;
            auntUncle.getValue().getFamilyMember().Dadid = 3;
            auntUncle.getValue().getFamilyMember().Momid = 4;
        }
        for (Entry<String, Person> auntUncle : this.PaternalAuntsUncles.entrySet()) {
            auntUncle.getValue().getFamilyMember().ID = i++;
            auntUncle.getValue().getFamilyMember().Dadid = 1;
            auntUncle.getValue().getFamilyMember().Momid = 2;
        }
    }

    /**
     * Adjusts proband gender if marked as 'Other' and has children.
     * Kinship2 pedigree tools cannot model an 'Other' parent, so the proband
     * is modeled as female with an unknown partner for visualization purposes.
     */
    private void setProbandSexIfOtherAndParent() {
        //Kindship2 can not model an other parent.
        // if the proband is other and parent
        // Model them as the mother with an unknown partner
        if (this.Proband.getGender().equalsIgnoreCase("Other") && !this.Children.isEmpty()) {
            this.Proband.setGender("Female");
        }
    }

    /**
     * Adds missing parent entries to complete the family structure.
     * Ensures each person has both mother and father entries, adding unknown
     * parent placeholders where necessary for pedigree completeness.
     */
    private void addMissingParents() {
        //If we have one of the parents add the other as unknown.
        if (this.Father == null && this.Mother != null) {
            this.Father = getUnknownPerson(MALE);
            this.Father.getFamilyMember().ID = 5;
        } else if (this.Mother == null && this.Father != null) {
            this.Mother = getUnknownPerson(FEMALE);
            this.Mother.getFamilyMember().ID = 6;
        } else if (this.Father == null && this.Mother == null) {
            this.Father = getUnknownPerson(MALE);
            this.Father.getFamilyMember().ID = 5;
            this.Mother = getUnknownPerson(FEMALE);
            this.Mother.getFamilyMember().ID = 6;
        }

        // In order for the pedigree drawing to work we need parents and/or Grandparents.
        if (this.PaternalGrandmother != null || this.PaternalGrandfather != null || !this.PaternalAuntsUncles.isEmpty()) {
            //We have at least one Paternal grandparent. So we need to have both grandparents and a Father.
            if (this.PaternalGrandfather == null) {
                this.PaternalGrandfather = getUnknownPerson(MALE);
                this.PaternalGrandfather.getFamilyMember().ID = 1;
            }

            if (this.PaternalGrandmother == null) {
                this.PaternalGrandmother = getUnknownPerson(FEMALE);
                this.PaternalGrandmother.getFamilyMember().ID = 2;
            }

            if (this.Father == null) {
                this.Father = getUnknownPerson(MALE);
                this.Father.getFamilyMember().ID = 5;
                this.Father.getFamilyMember().Dadid = 1;
                this.Father.getFamilyMember().Momid = 2;
            }

        }
        if (this.MaternalGrandmother != null || this.MaternalGrandfather != null || !this.MaternalAuntsUncles.isEmpty()) {
            //We have at least one Paternal grandparent. So we need to have both grandparents and a Father.
            if (this.MaternalGrandfather == null) {
                this.MaternalGrandfather = getUnknownPerson(MALE);
                this.MaternalGrandfather.getFamilyMember().ID = 3;
            }

            if (this.MaternalGrandmother == null) {
                this.MaternalGrandmother = getUnknownPerson(FEMALE);
                this.MaternalGrandmother.getFamilyMember().ID = 4;
            }

            if (this.Mother == null) {
                this.Mother = getUnknownPerson(FEMALE);
                this.Mother.getFamilyMember().ID = 6;
                this.Mother.getFamilyMember().Dadid = 3;
                this.Mother.getFamilyMember().Momid = 4;
            }

        }

    }

    /**
     * Compiles the complete list of all family members in the structure.
     * Includes all known and unknown family members with appropriate names assigned.
     *
     * @return list of all FamilyMember objects in the family
     */
    private List<FamilyMember> getFamilyMembers() {
        List<FamilyMember> members = new ArrayList<FamilyMember>();

        if (this.UnknownWife != null) {
            this.UnknownWife.getFamilyMember().name = "Unknown";
            members.add(this.UnknownWife.getFamilyMember());
        }

        if (this.UnknownHusband != null) {
            this.UnknownHusband.getFamilyMember().name = "Unknown";
            members.add(this.UnknownHusband.getFamilyMember());
        }

        if (this.UnknownMother != null) {
            this.UnknownMother.getFamilyMember().name = "Unknown_Mother";
            members.add(this.UnknownMother.getFamilyMember());
        }

        if (this.UnknownFather != null) {
            this.UnknownFather.getFamilyMember().name = "Unknown_Father";
            members.add(this.UnknownFather.getFamilyMember());
        }

        if (this.PaternalGrandfather != null) {
            this.PaternalGrandfather.getFamilyMember().name = "Grandfather";
            members.add(this.PaternalGrandfather.getFamilyMember());
        }
        if (this.PaternalGrandmother != null) {
            this.PaternalGrandmother.getFamilyMember().name = "Grandmother";
            members.add(this.PaternalGrandmother.getFamilyMember());
        }
        if (this.MaternalGrandfather != null) {
            this.MaternalGrandfather.getFamilyMember().name = "Grandfather";
            members.add(this.MaternalGrandfather.getFamilyMember());
        }
        if (this.MaternalGrandmother != null) {
            this.MaternalGrandmother.getFamilyMember().name = "Grandmother";
            members.add(this.MaternalGrandmother.getFamilyMember());
        }
        if (this.Father != null) {
            this.Father.getFamilyMember().name = "Father";
            members.add(this.Father.getFamilyMember());
        }
        if (this.Mother != null) {
            this.Mother.getFamilyMember().name = "Mother";
            members.add(this.Mother.getFamilyMember());
        }
        this.Proband.getFamilyMember().name = "Respondent";
        members.add(this.Proband.getFamilyMember());

        int i = 1;
        for (Entry<String, Person> child : this.Children.entrySet()) {
            child.getValue().getFamilyMember().name = "Child_" + i;
            i++;
            members.add(child.getValue().getFamilyMember());
        }

        i = 1;
        for (Entry<String, Person> sibling : this.Siblings.entrySet()) {
            sibling.getValue().getFamilyMember().name = "Sibling_" + i;
            i++;
            members.add(sibling.getValue().getFamilyMember());
        }

        int u = 1;
        int a = 1;
        int s = 1;
        for (Entry<String, Person> auntUncle : this.MaternalAuntsUncles.entrySet()) {
            if (auntUncle.getValue().getFamilyMember().Sex == 1) {
                auntUncle.getValue().getFamilyMember().name = "Uncle_" + u;
                u++;
            } else if (auntUncle.getValue().getFamilyMember().Sex == 2) {
                auntUncle.getValue().getFamilyMember().name = "Aunt_" + a;
                a++;
            } else {
                auntUncle.getValue().getFamilyMember().name = "Sibling_" + s;
                s++;
            }

            members.add(auntUncle.getValue().getFamilyMember());
        }

        u = 1;
        a = 1;
        s = 1;
        for (Entry<String, Person> auntUncle : this.PaternalAuntsUncles.entrySet()) {
            if (auntUncle.getValue().getFamilyMember().Sex == 1) {
                auntUncle.getValue().getFamilyMember().name = "Uncle_" + u;
                u++;
            } else if (auntUncle.getValue().getFamilyMember().Sex == 2) {
                auntUncle.getValue().getFamilyMember().name = "Aunt_" + a;
                a++;
            } else {
                auntUncle.getValue().getFamilyMember().name = "Sibling_" + s;
                s++;
            }
            members.add(auntUncle.getValue().getFamilyMember());
        }

        return members;
    }

    /**
     * Gets an unknown person placeholder with the specified sex.
     * Used to create placeholder family members when information is incomplete.
     *
     * @param sex the gender of the unknown person
     * @return a Person object configured as unknown
     */
    private Person getUnknownPerson(String sex) {
        Person p = new Person();
        p.setGender(sex);
        p.getFamilyMember().unknown = true;
        return p;
    }

    /**
     * Adds an unknown father entry to the family structure.
     * Creates a placeholder male parent when father information is not available.
     */
    private void addUnknownFather() {
        if (this.UnknownFather == null) {
            this.UnknownFather = getUnknownPerson(MALE);
            this.UnknownFather.getFamilyMember().ID = -1;
        }
    }

    /**
     * Adds an unknown mother entry to the family structure.
     * Creates a placeholder female parent when mother information is not available.
     */
    private void addUnknownMother() {
        if (this.UnknownMother == null) {
            this.UnknownMother = getUnknownPerson(FEMALE);
            this.UnknownMother.getFamilyMember().ID = -2;
        }
    }

    /**
     * Adds an unknown husband entry to the family structure.
     * Creates a placeholder male spouse when husband information is not available.
     */
    private void addUnknownHusband() {
        if (this.UnknownHusband == null) {
            this.UnknownHusband = getUnknownPerson(MALE);
            this.UnknownHusband.getFamilyMember().ID = -3;
        }
    }

    /**
     * Adds an unknown wife entry to the family structure.
     * Creates a placeholder female spouse when wife information is not available.
     */
    private void addUnknownWife() {
        if (this.UnknownWife == null) {
            this.UnknownWife = getUnknownPerson(FEMALE);
            this.UnknownWife.getFamilyMember().ID = -4;
        }
    }

    /**
     * Constructs the complete family object containing all family members.
     * <p>
     * This method is called after all family members have been processed and
     * their relationships established. It compiles the final family structure
     * used for pedigree generation.
     * </p>
     *
     * @return the complete Family object representing the proband's family
     */
    public Family getFamily() {

        setIDs();
        this.family = new Family();
        this.family.family.addAll(getFamilyMembers());
        return this.family;
    }
}
