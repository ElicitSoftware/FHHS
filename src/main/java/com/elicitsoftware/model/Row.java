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

public class Row {
    private static final String UKN_AGE = "Unknown";
    private final String title;
    private final String label;
    private final String value;
    public Row(String Title, String label, Integer value) {
        super();
        this.title = Title;
        this.label = label;

        if (value != null) {
            this.value = String.valueOf(value);
        } else {
            this.value = UKN_AGE;
        }
    }
    public Row(String Title, String label, String value) {
        super();
        this.title = Title;
        this.label = label;

        if (value != null) {
            this.value = value;
        } else {
            this.value = UKN_AGE;
        }
    }

    public String getTitle() {
        return title;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public String getHTML() {
        return "<span class=\"card--content--label\">" + label + "</span>" + "<span class=\"card--content--response\">: " + value + "</span><br/>";
    }

}
