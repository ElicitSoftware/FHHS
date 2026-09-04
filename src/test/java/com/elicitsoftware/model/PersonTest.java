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
 * Tests for {@link Person}, which mirrors data onto its linked {@link FamilyMember}
 * for every setter - the two objects must always stay in sync.
 */
class PersonTest {

    @Test
    void setGender_male_setsLowercaseGenderAndFamilyMemberSexOne() {
        Person p = new Person();
        p.setGender("Male");
        assertEquals("male", p.getGender());
        assertEquals(1, p.getFamilyMember().Sex);
    }

    @Test
    void setGender_female_setsFamilyMemberSexTwo() {
        Person p = new Person();
        p.setGender("FEMALE");
        assertEquals(2, p.getFamilyMember().Sex);
    }

    @Test
    void setGender_other_setsFamilyMemberSexThree() {
        Person p = new Person();
        p.setGender("Other");
        assertEquals(3, p.getFamilyMember().Sex);
    }

    @Test
    void setGender_unrecognizedValue_leavesFamilyMemberSexAtDefault() {
        Person p = new Person();
        p.setGender("nonbinary");
        assertEquals("nonbinary", p.getGender());
        assertEquals(3, p.getFamilyMember().Sex, "default Sex=3 must be unchanged for unmapped values");
    }

    @Test
    void setGender_nullOrEmpty_isIgnored() {
        Person p = new Person();
        p.setGender(null);
        assertNull(p.getGender());
        p.setGender("");
        assertNull(p.getGender(), "empty string is guarded out just like null, so Gender stays unset");
    }

    @Test
    void setVitalStatus_alive_setsFamilyMemberStatusZero() {
        Person p = new Person();
        p.getFamilyMember().Status = 1;
        p.setVital_Status("Alive");
        assertEquals(0, p.getFamilyMember().Status);
    }

    @Test
    void setVitalStatus_deceased_setsFamilyMemberStatusOne() {
        Person p = new Person();
        p.setVital_Status("Deceased");
        assertEquals(1, p.getFamilyMember().Status);
    }

    @Test
    void setAge_nonNull_setsAgeStringOnBothPersonAndFamilyMember() {
        Person p = new Person();
        p.setAge(42);
        assertEquals("42", p.getAge());
        assertEquals("42", p.getFamilyMember().Age);
    }

    @Test
    void setAge_null_leavesAgeUnset() {
        Person p = new Person();
        p.setAge(null);
        assertNull(p.getAge());
        assertNull(p.getFamilyMember().Age);
    }

    @Test
    void setAshkenazi_replacesSpacesWithUnderscoresOnFamilyMemberOnly() {
        Person p = new Person();
        p.setAshkenazi("Both Parents");
        assertEquals("Both_Parents", p.getFamilyMember().Ashkenazi);
    }

    @Test
    void setBreastCancer_propagatesRawValueToFamilyMember() {
        Person p = new Person();
        p.setBreast_Cancer("45");
        assertEquals("45", p.getFamilyMember().Breast_Cancer);
    }

    @Test
    void setSharedParent_isStoredOnPersonOnly() {
        Person p = new Person();
        p.setSharedParent("father");
        assertEquals("father", p.getSharedParent());
    }

    @Test
    void setOtherCancerType_propagatesToFamilyMember() {
        Person p = new Person();
        p.setOther_Cancer_Type("Sarcoma");
        assertEquals("Sarcoma", p.getFamilyMember().Other_Cancer_Type);
    }
}
