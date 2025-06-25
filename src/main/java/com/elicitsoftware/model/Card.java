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

/**
 * Represents a UI card component for displaying structured data in reports.
 * <p>
 * This class creates a card-like structure containing a title and a collection
 * of rows. It's used to organize and display family history health survey data
 * in a visually appealing HTML format. The card includes header and content
 * sections with CSS classes for styling.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class Card {

    /**
     * The title displayed in the card header.
     */
    private final String title;

    /**
     * A linked hash map of rows organized by label for ordered display.
     * Uses LinkedHashMap to maintain insertion order of the rows.
     */
    private final LinkedHashMap<String, Row> rows = new LinkedHashMap<String, Row>();

    /**
     * Constructs a new Card with the specified title.
     *
     * @param title the title to be displayed in the card header
     */
    public Card(String title) {
        super();
        this.title = title;
    }

    /**
     * Adds a list of rows to the card.
     * <p>
     * Each row is added to the internal map using a processed version of its label
     * as the key (with "Age" text removed and trimmed). This ensures consistent
     * labeling and removes redundant text.
     * </p>
     *
     * @param rowList the list of Row objects to add to the card
     */
    public void addRows(List<Row> rowList) {
        for (Row row : rowList) {
            this.rows.put(row.getLabel().replace("Age", "").trim(), row);
        }
    }

    /**
     * Generates the HTML representation of the card.
     * <p>
     * Creates a complete HTML structure with:
     * <ul>
     *   <li>A card container with CSS class "card"</li>
     *   <li>A header section with the card title</li>
     *   <li>A content section containing all rows</li>
     *   <li>Proper HTML formatting and separators</li>
     * </ul>
     *
     * @return a String containing the complete HTML representation of the card
     */
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
