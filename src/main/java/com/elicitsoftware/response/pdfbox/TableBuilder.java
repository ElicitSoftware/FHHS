package com.elicitsoftware.response.pdfbox;

/*-
 * ***LICENSE_START***
 * Elicit Survey
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.List;

/**
 * Builder class for constructing Table objects.
 * <p>
 * Provides a fluent interface for configuring table properties before
 * building the final Table instance.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class TableBuilder {

    /**
     * Default constructor.
     */
    public TableBuilder() {
        // Default constructor
    }

    /**
     * The table being built.
     */
    private final Table table = new Table();

    /**
     * Sets the table height.
     *
     * @param height the height to set
     * @return this builder for chaining
     */
    public TableBuilder setHeight(float height) {
        table.setHeight(height);
        return this;
    }

    /**
     * Sets the number of rows.
     *
     * @param numberOfRows the number of rows to set
     * @return this builder for chaining
     */
    public TableBuilder setNumberOfRows(Integer numberOfRows) {
        table.setNumberOfRows(numberOfRows);
        return this;
    }

    /**
     * Sets the row height.
     *
     * @param rowHeight the row height to set
     * @return this builder for chaining
     */
    public TableBuilder setRowHeight(float rowHeight) {
        table.setRowHeight(rowHeight);
        return this;
    }

    /**
     * Sets the table content.
     *
     * @param content the content to set
     * @return this builder for chaining
     */
    public TableBuilder setContent(String[][] content) {
        table.setContent(content);
        return this;
    }

    /**
     * Sets the columns.
     *
     * @param columns the columns to set
     * @return this builder for chaining
     */
    public TableBuilder setColumns(List<Column> columns) {
        table.setColumns(columns);
        return this;
    }

    /**
     * Sets the cell margin.
     *
     * @param cellMargin the cell margin to set
     * @return this builder for chaining
     */
    public TableBuilder setCellMargin(float cellMargin) {
        table.setCellMargin(cellMargin);
        return this;
    }

    /**
     * Sets the table margin.
     *
     * @param margin the margin to set
     * @return this builder for chaining
     */
    public TableBuilder setMargin(float margin) {
        table.setMargin(margin);
        return this;
    }

    /**
     * Sets the page size.
     *
     * @param pageSize the page size to set
     * @return this builder for chaining
     */
    public TableBuilder setPageSize(PDRectangle pageSize) {
        table.setPageSize(pageSize);
        return this;
    }

    /**
     * Sets the landscape orientation.
     *
     * @param landscape true for landscape, false for portrait
     * @return this builder for chaining
     */
    public TableBuilder setLandscape(boolean landscape) {
        table.setLandscape(landscape);
        return this;
    }

    /**
     * Sets the text font.
     *
     * @param textFont the font to set
     * @return this builder for chaining
     */
    public TableBuilder setTextFont(PDFont textFont) {
        table.setTextFont(textFont);
        return this;
    }

    /**
     * Sets the font size.
     *
     * @param fontSize the font size to set
     * @return this builder for chaining
     */
    public TableBuilder setFontSize(float fontSize) {
        table.setFontSize(fontSize);
        return this;
    }

    /**
     * Builds and returns the configured Table.
     *
     * @return the built Table instance
     */
    public Table build() {
        return table;
    }
}
