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
 * Represents a table structure for PDF generation using PDFBox.
 * <p>
 * This class encapsulates all properties needed to render a table in a PDF document,
 * including dimensions, styling, and content. It is typically built using the
 * TableBuilder class.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class Table {

    /**
     * Margin around the table.
     */
    private float margin;

    /**
     * Total height available for the table.
     */
    private float height;

    /**
     * Page size for the table.
     */
    private PDRectangle pageSize;

    /**
     * Whether the table is in landscape orientation.
     */
    private boolean isLandscape;

    /**
     * Height of each row in the table.
     */
    private float rowHeight;

    /**
     * Font used for table text.
     */
    private PDFont textFont;

    /**
     * Font size for table text.
     */
    private float fontSize;

    /**
     * Number of rows in the table.
     */
    private Integer numberOfRows;

    /**
     * List of column definitions.
     */
    private List<Column> columns;

    /**
     * Two-dimensional array containing table content.
     */
    private String[][] content;

    /**
     * Margin within table cells.
     */
    private float cellMargin;

    /**
     * Default constructor.
     */
    public Table() {
    }

    /**
     * Gets the number of columns in the table.
     *
     * @return number of columns
     */
    public Integer getNumberOfColumns() {
        return this.getColumns().size();
    }

    /**
     * Calculates the total width of the table.
     *
     * @return total table width
     */
    public float getWidth() {
        float tableWidth = 0f;
        for (Column column : columns) {
            tableWidth += column.getWidth();
        }
        return tableWidth;
    }

    /**
     * Gets the margin around the table.
     *
     * @return table margin
     */
    public float getMargin() {
        return margin;
    }

    /**
     * Sets the margin around the table.
     *
     * @param margin the margin to set
     */
    public void setMargin(float margin) {
        this.margin = margin;
    }

    /**
     * Gets the page size.
     *
     * @return page size
     */
    public PDRectangle getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size.
     *
     * @param pageSize the page size to set
     */
    public void setPageSize(PDRectangle pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Gets the text font.
     *
     * @return text font
     */
    public PDFont getTextFont() {
        return textFont;
    }

    /**
     * Sets the text font.
     *
     * @param textFont the font to set
     */
    public void setTextFont(PDFont textFont) {
        this.textFont = textFont;
    }

    /**
     * Gets the font size.
     *
     * @return font size
     */
    public float getFontSize() {
        return fontSize;
    }

    /**
     * Sets the font size.
     *
     * @param fontSize the font size to set
     */
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Gets column names as an array.
     *
     * @return array of column names
     */
    public String[] getColumnsNamesAsArray() {
        String[] columnNames = new String[getNumberOfColumns()];
        for (int i = 0; i < getNumberOfColumns(); i++) {
            columnNames[i] = columns.get(i).getName();
        }
        return columnNames;
    }

    /**
     * Gets the list of columns.
     *
     * @return list of columns
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * Sets the list of columns.
     *
     * @param columns the columns to set
     */
    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    /**
     * Gets the number of rows.
     *
     * @return number of rows
     */
    public Integer getNumberOfRows() {
        return numberOfRows;
    }

    /**
     * Sets the number of rows.
     *
     * @param numberOfRows the number of rows to set
     */
    public void setNumberOfRows(Integer numberOfRows) {
        this.numberOfRows = numberOfRows;
    }

    /**
     * Gets the table height.
     *
     * @return table height
     */
    public float getHeight() {
        return height;
    }

    /**
     * Sets the table height.
     *
     * @param height the height to set
     */
    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * Gets the row height.
     *
     * @return row height
     */
    public float getRowHeight() {
        return rowHeight;
    }

    /**
     * Sets the row height.
     *
     * @param rowHeight the row height to set
     */
    public void setRowHeight(float rowHeight) {
        this.rowHeight = rowHeight;
    }

    /**
     * Gets the table content.
     *
     * @return two-dimensional array of content
     */
    public String[][] getContent() {
        return content;
    }

    /**
     * Sets the table content.
     *
     * @param content the content to set
     */
    public void setContent(String[][] content) {
        this.content = content;
    }

    /**
     * Gets the cell margin.
     *
     * @return cell margin
     */
    public float getCellMargin() {
        return cellMargin;
    }

    /**
     * Sets the cell margin.
     *
     * @param cellMargin the cell margin to set
     */
    public void setCellMargin(float cellMargin) {
        this.cellMargin = cellMargin;
    }

    /**
     * Checks if the table is in landscape orientation.
     *
     * @return true if landscape, false otherwise
     */
    public boolean isLandscape() {
        return isLandscape;
    }

    /**
     * Sets the landscape orientation.
     *
     * @param isLandscape true for landscape, false for portrait
     */
    public void setLandscape(boolean isLandscape) {
        this.isLandscape = isLandscape;
    }
}
