package com.elicitsoftware.model;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

/**
 * Encodes text for placement inside the HTML a report builds (UC-001 BR-002, UC-002 BR-002).
 * <p>
 * The reports write their own markup and place survey text inside it: answer text, the name
 * of an "other" cancer, step names. That text is encoded here so that it is shown as typed.
 * The PDF tables read the same text from the getters and are not encoded.
 * </p>
 */
final class Html {

    private Html() {
    }

    /**
     * Encodes the five characters HTML treats as markup.
     *
     * @param text the text to place in HTML; {@code null} is treated as empty
     * @return the encoded text
     */
    static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
