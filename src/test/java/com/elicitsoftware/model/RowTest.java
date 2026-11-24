package com.elicitsoftware.model;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
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

    /** UC-001 BR-002, UC-002 BR-002: survey text is shown as typed, inside markup the report wrote. */
    @Test
    void getHTML_encodesMarkupCharactersInLabelAndValue() {
        Row row = new Row("Mother", "<img src=x onerror=alert(1)>", "a&b'c\"d");
        String html = row.getHTML();

        assertEquals(
                "<span class=\"card--content--label\">&lt;img src=x onerror=alert(1)&gt;</span>"
                        + "<span class=\"card--content--response\">: a&amp;b&#39;c&quot;d</span><br/>",
                html
        );
    }

    /** The PDF table reads the getters, which return the text as it is. */
    @Test
    void getters_returnTextUnencoded() {
        Row row = new Row("Mother", "<img src=x onerror=alert(1)>", "a&b'c\"d");

        assertEquals("<img src=x onerror=alert(1)>", row.getLabel());
        assertEquals("a&b'c\"d", row.getValue());
    }
}
