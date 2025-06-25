package com.elicitsoftware.model;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 The Regents of the Universi     * </ul>
     *
     * @return a formatted HTML string representing this rowf Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

/**
 * Represents a single row of data in a report or table structure.
 * <p>
 * This class models a key-value pair with an associated title, commonly used
 * in family history health survey reports. Each row contains a title, label,
 * and value, and can generate its own HTML representation for display purposes.
 * </p>
 * <p>
 * The class handles null values gracefully by substituting "Unknown" when
 * no value is provided, ensuring consistent data presentation.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class Row {

    /**
     * Default value used when age or other data is unknown.
     */
    private static final String UKN_AGE = "Unknown";

    /**
     * The title associated with this row.
     */
    private final String title;

    /**
     * The label or field name for this row.
     */
    private final String label;

    /**
     * The value or data content for this row.
     */
    private final String value;

    /**
     * Constructs a new Row with the specified title, label, and integer value.
     * <p>
     * If the value is null, it will be set to "Unknown" to maintain consistent
     * data presentation.
     * </p>
     *
     * @param Title the title for this row
     * @param label the label or field name
     * @param value the integer value, or null if unknown
     */
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

    /**
     * Constructs a new Row with the specified title, label, and string value.
     * <p>
     * If the value is null, it will be set to "Unknown" to maintain consistent
     * data presentation.
     * </p>
     *
     * @param Title the title for this row
     * @param label the label or field name
     * @param value the string value, or null if unknown
     */
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

    /**
     * Gets the title of this row.
     *
     * @return the row title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the label of this row.
     *
     * @return the row label or field name
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the value of this row.
     *
     * @return the row value, or "Unknown" if no value was provided
     */
    public String getValue() {
        return value;
    }

    /**
     * Generates the HTML representation of this row.
     * <p>
     * Creates a formatted HTML structure with CSS classes for styling:
     * <ul>
     *   <li>Label is wrapped in "card--content--label" class</li>
     *   <li>Value is wrapped in "card--content--response" class</li>
     *   <li>Includes proper formatting with colon separator and line break</li>
     * </ul>
     *
     * @return a formatted HTML string representing this row
     */
    public String getHTML() {
        return "<span class=\"card--content--label\">" + label + "</span>" + "<span class=\"card--content--response\">: " + value + "</span><br/>";
    }
}
