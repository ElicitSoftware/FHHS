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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void getHTML_includesTitleAndAllRowHtml() {
        Card card = new Card("Mother");
        card.addRows(List.of(new Row("Mother", "Breast Cancer", 45)));

        String html = card.getHTML();

        assertTrue(html.contains("<div class=\"card\">"));
        assertTrue(html.contains("<p>Mother</p>"));
        assertTrue(html.contains("Breast Cancer"));
        assertTrue(html.contains("45"));
    }

    @Test
    void addRows_stripsAgeSuffixFromKeyButNotFromDisplayedLabel() {
        // Card keys rows by label with "Age" removed & trimmed, primarily so a
        // "Breast Cancer" row and a same-named "...Age" row collapse to one slot.
        Card card = new Card("Sibling");
        card.addRows(List.of(
                new Row("Sibling", "Breast Cancer Age", 45),
                new Row("Sibling", "Breast Cancer", 99) // same de-suffixed key -> overwrites the first
        ));

        String html = card.getHTML();

        // Only the second row (which won on key collision) should render.
        assertTrue(html.contains("99"));
        assertFalse(html.contains("45"));
    }

    @Test
    void addRows_distinctLabels_bothRendered() {
        Card card = new Card("Proband");
        card.addRows(List.of(
                new Row("Proband", "Breast Cancer", 45),
                new Row("Proband", "Lung Cancer", 60)
        ));

        String html = card.getHTML();

        assertTrue(html.contains("Breast Cancer"));
        assertTrue(html.contains("Lung Cancer"));
    }
}
