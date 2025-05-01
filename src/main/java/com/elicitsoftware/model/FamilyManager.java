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

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.RequestScoped;

import javax.management.relation.RelationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

@RequestScoped
public class FamilyManager {
    private static final String UKN_AGE = "unk. age";
    private static final String STRING_TRUE = "true";
    private static final String MALE = "Male";
    private static final String FEMALE = "Female";

    // These family members are collections
    private final LinkedHashMap<String, Person> Children = new LinkedHashMap<>();
    private final LinkedHashMap<String, Person> Siblings = new LinkedHashMap<>();
    private final LinkedHashMap<String, Person> MaternalAuntsUncles = new LinkedHashMap<>();
    private final LinkedHashMap<String, Person> PaternalAuntsUncles = new LinkedHashMap<>();
    Family family = null;
    // These family members are limited to one.
    private Person Proband = null;
    private Person Mother = null;
    private Person Father = null;
    private Person MaternalGrandmother = null;
    private Person MaternalGrandfather = null;
    private Person PaternalGrandmother = null;
    private Person PaternalGrandfather = null;
    // For step siblings we need to add an their parent.
    private Person UnknownMother = null;
    private Person UnknownFather = null;
    // Add Proband's Spouse
    private Person UnknownHusband = null;
    private Person UnknownWife = null;

    public List<FactFHHSView> findByRespondentid(long respondentId) {
        return FactFHHSView.find("respondent_id =?1", Sort.by("relationship").and("step"), respondentId).list();
    }

    public Family getFamily(long id) {
        FamilyManager fm = new FamilyManager();

        List<FactFHHSView> rows = findByRespondentid(id);
        fm.addFamily(rows);

        return fm.getFamily();
    }

    public void addFamily(List<FactFHHSView> rows) {
        for (FactFHHSView fact : rows) {
            try {
                Person p = getPerson(fact);
                p.setAge(fact.age);
                p.setAshkenazi(fact.ashkenazi);
                p.setGender(fact.gender);
                p.setSharedParent(fact.shared_parent);
                p.setVital_Status(fact.vital_status);

                if (STRING_TRUE.equalsIgnoreCase(fact.bladder_cancer)) {
                    p.setBladder_Cancer(Objects.requireNonNullElse(fact.bladder_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Bladder_Cancer(fact.multiple_bladder_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.breast_cancer)) {
                    p.setBreast_Cancer(Objects.requireNonNullElse(fact.breast_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Breast_Cancer(fact.multiple_breast_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.colon_or_rectal_cancer)) {
                    p.setColon_Rectal_Cancer(Objects.requireNonNullElse(fact.colon_or_rectal_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Colon_Rectal_Cancer(fact.multiple_colon_or_rectal_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.endometrial_or_uterine_cancer)) {
                    p.setEndometrial_Uterine_Cancer(Objects.requireNonNullElse(fact.endometrial_or_uterine_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Endometrial_Uterine_Cancer(fact.multiple_endometrial_or_uterine_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.kidney_renal_cell_cancer)) {
                    p.setKidney_Renal_Cell_Cancer(Objects.requireNonNullElse(fact.kidney_renal_cell_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Kidney_Renal_Cell_Cancer(fact.multiple_kidney_renal_cell_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.leukemia)) {
                    p.setLeukemia_Cancer(Objects.requireNonNullElse(fact.leukemia_age, UKN_AGE).toString());
                    p.setMultiple_Leukemia_Cancer(fact.multiple_leukemias);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.lung_cancer)) {
                    p.setLung_Cancer(Objects.requireNonNullElse(fact.lung_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Lung_Cancer(fact.multiple_lung_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.lymphoma)) {
                    p.setLymphoma(Objects.requireNonNullElse(fact.lymphoma_age, UKN_AGE).toString());
                    p.setMultiple_Lymphoma(fact.multiple_lymphomas);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.melanoma_skin_cancer)) {
                    p.setMelanoma_Cancer(Objects.requireNonNullElse(fact.melanoma_skin_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Melanoma_Cancer(fact.multiple_melanoma_skin_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.nonmelanoma_skin_cancer)) {
                    p.setNon_Melanoma_Cancer(Objects.requireNonNullElse(fact.nonmelanoma_skin_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Non_Melanoma_Cancer(fact.multiple_nonmelanoma_skin_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.oral_cavity_or_throat_cancer)) {
                    p.setOral_Throat_Cancer(Objects.requireNonNullElse(fact.oral_cavity_or_throat_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Oral_Throat_Cancer(fact.multiple_oral_cavity_or_throat_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.other_cancer)) {
                    p.setOther_Cancer(Objects.requireNonNullElse(fact.other_age, UKN_AGE).toString());
                    p.setOther_Cancer_Type(fact.other_cancer_name);
                    p.setMultiple_Other_Cancer(fact.multiple_other_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.ovarian_cancer)) {
                    p.setOvarian_Cancer(Objects.requireNonNullElse(fact.ovarian_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Ovarian_Cancer(fact.multiple_ovarian_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.pancreatic_cancer)) {
                    p.setPancreatic_Cancer(Objects.requireNonNullElse(fact.pancreatic_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Pancreatic_Cancer(fact.multiple_pancreatic_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.prostate_cancer)) {
                    p.setProstate_Cancer(Objects.requireNonNullElse(fact.prostate_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Prostate_Cancer(fact.multiple_prostate_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.stomach_cancer)) {
                    p.setStomach_Cancer(Objects.requireNonNullElse(fact.stomach_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Stomach_Cancer(fact.multiple_stomach_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.testicular_cancer)) {
                    p.setTesticular_Cancer(Objects.requireNonNullElse(fact.testicular_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Testicular_Cancer(fact.multiple_testicular_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.thyroid_cancer)) {
                    p.setThyroid_Cancer(Objects.requireNonNullElse(fact.thyroid_cancer_age, UKN_AGE).toString());
                    p.setMultiple_Thyroid_Cancer(fact.multiple_thyroid_cancers);
                }
                if (STRING_TRUE.equalsIgnoreCase(fact.unknown_cancer)) {
                    p.setUnknown_Cancer(Objects.requireNonNullElse(fact.unknown_cancer_age, UKN_AGE).toString());
                }
            } catch (RelationException e) {
                // There are some steps that are not relationships. i.e. Demographics
            }
        }
    }

    private Person getPerson(FactFHHSView fact) throws RelationException {
        String key = fact.step + Objects.requireNonNullElse(fact.step_instance, "");
        switch (fact.step) {
            case "Proband":
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

    private void setProbandSexIfOtherAndParent() {
        //Kindship2 can not model an other parent.
        // if the proband is other and parent
        // Model them as the mother with an unknown partner
        if (this.Proband.getGender().equalsIgnoreCase("Other") && !this.Children.isEmpty()) {
            this.Proband.setGender("Female");
        }
    }


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

    private Person getUnknownPerson(String sex) {
        Person p = new Person();
        p.setGender(sex);
        p.getFamilyMember().unknown = true;
        return p;
    }

    private void addUnknownFather() {
        if (this.UnknownFather == null) {
            this.UnknownFather = getUnknownPerson(MALE);
            this.UnknownFather.getFamilyMember().ID = -1;
        }
    }

    private void addUnknownMother() {
        if (this.UnknownMother == null) {
            this.UnknownMother = getUnknownPerson(FEMALE);
            this.UnknownMother.getFamilyMember().ID = -2;
        }
    }

    private void addUnknownHusband() {
        if (this.UnknownHusband == null) {
            this.UnknownHusband = getUnknownPerson(MALE);
            this.UnknownHusband.getFamilyMember().ID = -3;
        }
    }

    private void addUnknownWife() {
        if (this.UnknownWife == null) {
            this.UnknownWife = getUnknownPerson(FEMALE);
            this.UnknownWife.getFamilyMember().ID = -4;
        }
    }

    public Family getFamily() {

        setIDs();
        this.family = new Family();
        this.family.family.addAll(getFamilyMembers());
        return this.family;
    }
}
