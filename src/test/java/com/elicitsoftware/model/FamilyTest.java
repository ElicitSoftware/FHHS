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

class FamilyTest {

    @Test
    void hasMultipleCancers_falseForEmptyFamily() {
        assertFalse(new Family().hasMultipleCancers());
    }

    @Test
    void hasMultipleCancers_trueWhenAnyMemberHasMultipleFlag() {
        Family family = new Family();
        FamilyMember withoutMultiple = new FamilyMember();
        FamilyMember withMultiple = new FamilyMember();
        withMultiple.Multiple_Lung_Cancer = "true";
        family.family.add(withoutMultiple);
        family.family.add(withMultiple);

        assertTrue(family.hasMultipleCancers());
    }

    @Test
    void toString_startsWithPedigreeHeaderRow() {
        Family family = new Family();
        String output = family.toString();

        assertTrue(output.startsWith("famid\tid\tsex\tdadid\tmomid\tdeceased\tproband\taffection\tavail\tdisplay_id\tcancer_label"));
    }

    @Test
    void toString_appendsOneLinePerFamilyMember() {
        Family family = new Family();
        FamilyMember m1 = new FamilyMember();
        m1.ID = 1;
        m1.name = "A";
        FamilyMember m2 = new FamilyMember();
        m2.ID = 2;
        m2.name = "B";
        family.family.add(m1);
        family.family.add(m2);

        String output = family.toString();
        long lineCount = output.lines().count();

        assertEquals(3, lineCount, "header + one line per member");
    }
}
