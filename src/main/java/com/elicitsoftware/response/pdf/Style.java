package com.elicitsoftware.response.pdf;

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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents styling configuration for PDF document elements.
 * <p>
 * This class defines various formatting properties that can be applied to
 * content elements in PDF documents, including text alignment, font styling,
 * colors, and spacing. It provides a flexible way to customize the appearance
 * of different document elements.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Style {

    /**
     * Text alignment for the styled element.
     * Common values include "left", "center", "right", and "justify".
     */
    public String alignment;

    /**
     * Indicates whether the text should be rendered in bold.
     * True for bold text, false or null for normal weight.
     */
    public Boolean bold;

    /**
     * Color specification for the text or element.
     * Can be specified as hex values (e.g., "#FF0000") or named colors.
     */
    public String color;

    /**
     * Font size in points for text elements.
     * Larger values produce bigger text.
     */
    public Integer fontSize;

    /**
     * Margin settings as an array of integers.
     * Typically follows CSS-style ordering: [top, right, bottom, left]
     * or [vertical, horizontal] depending on array length.
     */
    public Integer[] margin;
}
