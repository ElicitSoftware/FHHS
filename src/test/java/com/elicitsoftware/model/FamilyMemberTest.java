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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FamilyMember}, focused on the pedigree-string serialization
 * ({@code toString()}) and {@code hasMultipleCancers()} logic consumed by the
 * pedigree-generation pipeline.
 */
class FamilyMemberTest {

    private FamilyMember probandWithNoCancer() {
        FamilyMember m = new FamilyMember();
        m.ID = 7;
        m.Sex = 2;
        m.name = "Respondent";
        return m;
    }

    @Test
    void toString_defaultUnknownMember_hasNaParentsAndFalseFlags() {
        FamilyMember m = new FamilyMember();
        m.ID = 1;
        m.Dadid = 0;
        m.Momid = 0;
        m.name = "Grandfather";

        String line = m.toString();
        String[] fields = line.split("\t");

        assertEquals("1", fields[0]); // famid
        assertEquals("1", fields[1]); // id
        assertEquals("3", fields[2]); // sex default (other/unknown)
        assertEquals("NA", fields[3]); // dadid=0 -> NA
        assertEquals("NA", fields[4]); // momid=0 -> NA
        assertEquals("false", fields[5]); // deceased (Status != 1)
        assertEquals("false", fields[6]); // proband (ID != 7)
        assertEquals("false", fields[7]); // affection (no cancer)
    }

    @Test
    void toString_probandId_marksProbandColumnTrue() {
        FamilyMember m = probandWithNoCancer();
        String[] fields = m.toString().split("\t");
        assertEquals("true", fields[6]);
    }

    @Test
    void toString_deceasedStatus_marksDeceasedColumnTrue() {
        FamilyMember m = probandWithNoCancer();
        m.Status = 1;
        String[] fields = m.toString().split("\t");
        assertEquals("true", fields[5]);
    }

    @Test
    void toString_withCancer_marksAffectionColumnTrueAndIncludesCancerLabel() {
        FamilyMember m = probandWithNoCancer();
        m.Breast_Cancer = "45";
        String line = m.toString();
        String[] fields = line.split("\t");

        assertEquals("true", fields[7]); // affection
        assertTrue(line.contains("Breast"));
        assertTrue(line.contains("45"));
    }

    @Test
    void toString_multipleCancerFlag_appendsAsteriskToLabel() {
        FamilyMember m = probandWithNoCancer();
        m.Breast_Cancer = "45";
        m.Multiple_Breast_Cancer = "true";

        String line = m.toString();

        assertTrue(line.contains("Breast*"), "multiple-diagnosis marker '*' must be present: " + line);
    }

    @Test
    void toString_displayId_includesNameAndAgeForNonProband() {
        FamilyMember m = new FamilyMember();
        m.ID = 5;
        m.name = "Father";
        m.Age = "68";

        String line = m.toString();

        assertTrue(line.contains("Father (Age 68)"), "expected display id in: " + line);
    }

    @Test
    void toString_displayId_probandUsesRespondentProbandLabelRegardlessOfName() {
        FamilyMember m = new FamilyMember();
        m.ID = 7;
        m.name = "Respondent";
        m.Age = "40";

        String line = m.toString();

        assertTrue(line.contains("Respondent (Proband) (Age 40)"), "expected in: " + line);
    }

    @Test
    void toString_unknownMember_neverShowsCancerLabelEvenIfFieldsSet() {
        // hasCancer()/buildCancerLabel() checks the "unknown" placeholder flag first.
        FamilyMember m = new FamilyMember();
        m.ID = 99;
        m.unknown = true;
        m.Breast_Cancer = "45";

        String line = m.toString();
        String[] fields = line.split("\t");

        // hasCancer() itself still reports true (used for the "affection" column)...
        assertEquals("true", fields[7]);
        // ...but buildCancerLabel() suppresses the textual label for unknown placeholders.
        assertFalse(line.contains("Breast"));
    }

    @Test
    void hasMultipleCancers_falseWhenNoFlagsSet() {
        FamilyMember m = new FamilyMember();
        assertFalse(m.hasMultipleCancers());
    }

    @Test
    void hasMultipleCancers_trueWhenAnySingleFlagIsTrueCaseInsensitive() {
        FamilyMember m = new FamilyMember();
        m.Multiple_Thyroid_Cancer = "TRUE";
        assertTrue(m.hasMultipleCancers());
    }

    @Test
    void hasMultipleCancers_falseWhenFlagPresentButNotTrue() {
        FamilyMember m = new FamilyMember();
        m.Multiple_Thyroid_Cancer = "false";
        assertFalse(m.hasMultipleCancers());
    }
}
