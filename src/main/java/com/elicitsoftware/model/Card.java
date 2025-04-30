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

import java.util.LinkedHashMap;
import java.util.List;

public class Card {

    private final String title;
    private final LinkedHashMap<String, Row> rows = new LinkedHashMap<String, Row>();

    public Card(String title) {
        super();
        this.title = title;
    }

    public void addRows(List<Row> rowList) {
        for (Row row : rowList) {
            this.rows.put(row.getLabel().replace("Age", "").trim(), row);
        }
    }

    public String getHTML() {
        StringBuffer html = new StringBuffer();
        html.append("<div class=\"card\">");
        html.append("<div class=\"card--header\"><p>" + this.title + "</p></div><div class=\"card--content\">");


        for (Row row : rows.values()) {
            html.append(row.getHTML());
        }
        html.append("<br/><hr/>");
        html.append("</div>");
        html.append("</div>");
        return html.toString();
    }
}
