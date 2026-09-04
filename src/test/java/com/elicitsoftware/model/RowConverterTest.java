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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RowConverter#toRows(FamilyHistoryRecord)}.
 */
class RowConverterTest {

    /** Mirrors the 66-arg constructor of {@link FamilyHistoryRecord}, defaulting everything to null. */
    private static FamilyHistoryRecord blank(String step) {
        return new FhrBuilder(step).build();
    }

    @Test
    void nullRecord_returnsEmptyList() {
        assertTrue(RowConverter.toRows(null).isEmpty());
    }

    @Test
    void noCancerFlags_returnsEmptyList() {
        assertTrue(RowConverter.toRows(blank("Proband")).isEmpty());
    }

    @Test
    void breastCancerTrue_producesThreeRowsIncludingTripleNegative() {
        FamilyHistoryRecord r = new FhrBuilder("Sibling")
                .breastCancer("true", 45, "false")
                .tripleNegative("true")
                .build();

        List<Row> rows = RowConverter.toRows(r);

        assertEquals(3, rows.size());
        assertEquals("Breast Cancer", rows.get(0).getLabel());
        assertEquals("45", rows.get(0).getValue());
        assertEquals("Sibling", rows.get(0).getTitle());
        assertEquals("Triple Negative Breast Cancer", rows.get(1).getLabel());
        assertEquals("Multiple Breast Cancers", rows.get(2).getLabel());
        assertEquals("false", rows.get(2).getValue());
    }

    @Test
    void breastCancerCaseInsensitive_stillMatches() {
        FamilyHistoryRecord r = new FhrBuilder("Proband").breastCancer("TRUE", 45, null).build();
        assertFalse(RowConverter.toRows(r).isEmpty());
    }

    @Test
    void breastCancerFalse_producesNoRows() {
        FamilyHistoryRecord r = new FhrBuilder("Proband").breastCancer("false", 45, null).build();
        assertTrue(RowConverter.toRows(r).isEmpty());
    }

    @Test
    void otherCancer_usesProvidedNameInRowLabelAndMultipleLabel() {
        FamilyHistoryRecord r = new FhrBuilder("Mother").other("true", 60, "Sarcoma", "true").build();

        List<Row> rows = RowConverter.toRows(r);

        assertEquals(2, rows.size());
        assertEquals("Sarcoma", rows.get(0).getLabel());
        assertEquals("60", rows.get(0).getValue());
        assertEquals("Multiple Sarcomas", rows.get(1).getLabel());
    }

    @Test
    void otherCancer_withNoNameFallsBackToOtherCancerLabel() {
        FamilyHistoryRecord r = new FhrBuilder("Mother").other("true", 60, null, "false").build();

        List<Row> rows = RowConverter.toRows(r);

        assertEquals("Other Cancer", rows.get(0).getLabel());
        assertEquals("Multiple Other Cancers", rows.get(1).getLabel());
    }

    @Test
    void unknownCancer_producesExactlyOneRowWithNoMultipleRow() {
        FamilyHistoryRecord r = new FhrBuilder("Father").unknownCancer("true", 70).build();

        List<Row> rows = RowConverter.toRows(r);

        assertEquals(1, rows.size());
        assertEquals("Unknown Cancer", rows.get(0).getLabel());
    }

    @Test
    void ashkenaziTrue_addsAncestryRowWithAshkenaziValueAsBothLabelValue() {
        FamilyHistoryRecord r = new FhrBuilder("Proband").ashkenazi("true").build();

        List<Row> rows = RowConverter.toRows(r);

        assertEquals(1, rows.size());
        assertEquals("Ashkenazi Ancestry", rows.get(0).getLabel());
        assertEquals("true", rows.get(0).getValue());
    }

    @Test
    void ashkenaziFalse_addsNoRow() {
        FamilyHistoryRecord r = new FhrBuilder("Proband").ashkenazi("false").build();
        assertTrue(RowConverter.toRows(r).isEmpty());
    }

    @Test
    void multipleCancerTypesOnSameRecord_eachContributeIndependently() {
        FamilyHistoryRecord r = new FhrBuilder("Proband")
                .breastCancer("true", 45, "false")
                .lungCancer("true", 55, "true")
                .build();

        List<Row> rows = RowConverter.toRows(r);

        // Breast Cancer unconditionally contributes 3 rows (age, triple-negative, multiple)
        // regardless of the triple-negative value itself; Lung Cancer contributes 2 (age, multiple).
        assertEquals(5, rows.size());
    }

    /**
     * Minimal fluent builder over the 66-arg {@link FamilyHistoryRecord} constructor,
     * used only to set the specific cancer fields each test cares about.
     */
    private static final class FhrBuilder {
        String step;
        String breastCancer; Integer breastCancerAge; String multipleBreastcancers; String tripleNegative;
        String lungCancer; Integer lungCancerAge; String multipleLungCancers;
        String otherCancer; Integer otherAge; String otherCancerName; String multipleOtherCancers;
        String unknownCancer; Integer unknownCancerAge;
        String ashkenazi;

        FhrBuilder(String step) { this.step = step; }

        FhrBuilder breastCancer(String v, Integer age, String multiple) {
            this.breastCancer = v; this.breastCancerAge = age; this.multipleBreastcancers = multiple; return this;
        }
        FhrBuilder tripleNegative(String v) { this.tripleNegative = v; return this; }
        FhrBuilder lungCancer(String v, Integer age, String multiple) {
            this.lungCancer = v; this.lungCancerAge = age; this.multipleLungCancers = multiple; return this;
        }
        FhrBuilder other(String v, Integer age, String name, String multiple) {
            this.otherCancer = v; this.otherAge = age; this.otherCancerName = name; this.multipleOtherCancers = multiple; return this;
        }
        FhrBuilder unknownCancer(String v, Integer age) {
            this.unknownCancer = v; this.unknownCancerAge = age; return this;
        }
        FhrBuilder ashkenazi(String v) { this.ashkenazi = v; return this; }

        FamilyHistoryRecord build() {
            return new FamilyHistoryRecord(
                    step, null, null, null, null,
                    null, null, ashkenazi,
                    null, null,
                    breastCancer, breastCancerAge, tripleNegative,
                    null, null,
                    null, null,
                    null, null,
                    null, null,
                    lungCancer, lungCancerAge,
                    null, null,
                    null, null,
                    null, null,
                    null, null,
                    otherCancer, otherAge, otherCancerName,
                    null, null,
                    null, null,
                    null, null,
                    null, null,
                    null, null,
                    null, null,
                    unknownCancer, unknownCancerAge,
                    null, multipleBreastcancers,
                    null, null,
                    null, null,
                    multipleLungCancers, null,
                    null, null,
                    null, multipleOtherCancers,
                    null, null,
                    null, null,
                    null, null
            );
        }
    }
}
