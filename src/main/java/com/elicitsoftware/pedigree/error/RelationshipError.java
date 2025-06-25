package com.elicitsoftware.pedigree.error;

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

/**
 * Exception thrown when errors occur during family relationship processing.
 * <p>
 * This exception is used to indicate problems with family relationship
 * data or processing, such as invalid relationship codes, conflicting
 * family structure information, or missing required relationship data
 * during pedigree generation.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class RelationshipError extends Exception {

    /**
     * Constructs a new RelationshipError with no detail message.
     */
    public RelationshipError() {
        super();
    }

    /**
     * Constructs a new RelationshipError with the specified detail message.
     *
     * @param message the detail message explaining the relationship error
     */
    public RelationshipError(String message) {
        super(message);
    }
}
