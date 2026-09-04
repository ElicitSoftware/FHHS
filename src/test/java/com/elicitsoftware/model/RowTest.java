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

class RowTest {

    @Test
    void integerConstructor_nonNullValue_isStringified() {
        Row row = new Row("Sibling", "Breast Cancer", 45);
        assertEquals("45", row.getValue());
    }

    @Test
    void integerConstructor_nullValue_becomesUnknown() {
        Row row = new Row("Sibling", "Breast Cancer", (Integer) null);
        assertEquals("Unknown", row.getValue());
    }

    @Test
    void stringConstructor_nullValue_becomesUnknown() {
        Row row = new Row("Proband", "Ashkenazi Ancestry", (String) null);
        assertEquals("Unknown", row.getValue());
    }

    @Test
    void stringConstructor_nonNullValue_isPreservedAsIs() {
        Row row = new Row("Proband", "Ashkenazi Ancestry", "true");
        assertEquals("true", row.getValue());
    }

    @Test
    void getters_returnConstructorArguments() {
        Row row = new Row("Mother", "Lung Cancer", 60);
        assertEquals("Mother", row.getTitle());
        assertEquals("Lung Cancer", row.getLabel());
    }

    @Test
    void getHTML_wrapsLabelAndValueInExpectedSpans() {
        Row row = new Row("Mother", "Lung Cancer", 60);
        String html = row.getHTML();

        assertEquals(
                "<span class=\"card--content--label\">Lung Cancer</span>"
                        + "<span class=\"card--content--response\">: 60</span><br/>",
                html
        );
    }
}
