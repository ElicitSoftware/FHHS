package com.elicitsoftware.model;

/*-
 * ***LICENSE_START***
 *     * </ul>
     *
     * @return a formatted string representation suitable for pedigree toolscit FHHS
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete family structure for family history health surveys.
 * <p>
 * This class encapsulates the entire family unit including all family members
 * and their relationships. It provides utility methods for analyzing family-wide
 * health patterns and generating pedigree representations suitable for
 * visualization and analysis tools.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class Family {

    /**
     * Default constructor.
     */
    public Family() {
        // Default constructor
    }

    /**
     * List of all family members included in this family structure.
     * Each FamilyMember contains health information, demographics, and relationship data.
     */
    public List<FamilyMember> family = new ArrayList<>();

    /**
     * Determines if any family member has multiple cancer diagnoses.
     * <p>
     * This method scans through all family members to identify if any individual
     * has been diagnosed with multiple instances of the same cancer type, which
     * is important for risk assessment and report formatting.
     * </p>
     *
     * @return true if any family member has multiple cancer diagnoses, false otherwise
     */
    public boolean hasMultipleCancers() {
        for (FamilyMember p : family) {
            if (p.hasMultipleCancers()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a pedigree-formatted string representation of the family.
     * <p>
     * Creates a text-based pedigree format that includes:
     * <ul>
     *   <li>Standard pedigree headers (Ped, ID, Sex, Dadid, Momid, etc.)</li>
     *   <li>Each family member's pedigree data on separate lines</li>
     *   <li>Formatted data suitable for pedigree generation tools</li>
     * </ul>
     *
     * @return a formatted string representation suitable for pedigree tools
     */
    @Override
    public String toString() {
        // Add the headers
        StringBuilder sb = new StringBuilder("Ped	ID	Sex	Dadid	Momid	Status	Label	ul	ur	ll	lr");
        sb.append(System.lineSeparator());
        for (FamilyMember p : family) {
            sb.append(p.toString());
        }
        return sb.toString();
    }
}
