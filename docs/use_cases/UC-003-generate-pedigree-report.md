# Use Case: Generate Pedigree Report

## Overview

**Use Case ID:** UC-003
**Use Case Name:** Generate Pedigree Report
**Primary Actor:** Survey Platform
**Goal:** Obtain a PDF report containing a visual family pedigree diagram with color-coded cancer indicators.
**Status:** Implemented

## Preconditions

- The respondent has recorded their family structure (relatives and cancer history) via the survey.

## Main Success Scenario

1. The Survey Platform requests a pedigree report for a respondent, identified by respondent ID.
2. The system assembles the respondent's family structure and cancer history into a family data set.
3. The system sends the family data to the external Pedigree Drawing Service and receives a rendered pedigree diagram.
4. The system builds a PDF containing the pedigree diagram, a color legend, and a note when any family member has multiple diagnoses of the same cancer type.
5. The system returns the report title, diagram content, and PDF document to the caller.

## Alternative Flows

### A1: Pedigree Drawing Service Is Unavailable or Returns an Error

**Trigger:** The external Pedigree Drawing Service does not respond successfully within 15 seconds, or returns content that isn't a valid diagram (BR-001) (step 3).
**Flow:**

1. The system substitutes a placeholder message for the diagram. Use case continues at step 4.

## Postconditions

### Success Postconditions

- A pedigree report (diagram + PDF) reflecting the respondent's family structure and cancer history is returned to the caller.

### Failure Postconditions

- A pedigree report is still returned, but with a placeholder in place of the diagram (see A1) — the use case has no hard failure postcondition.

## Business Rules

### BR-001: Pedigree Diagram Degrades Gracefully

If the external Pedigree Drawing Service fails, times out, or returns invalid diagram content, the report must still be generated, showing a placeholder message instead of the diagram rather than failing the request.

---

## Reference

### Status Values

| Status      | Description                                      |
|-------------|--------------------------------------------------|
| Draft       | Initial version, still being written.            |
| Reviewed    | Complete, awaiting stakeholder review.           |
| Approved    | Reviewed and approved for implementation.        |
| Implemented | Implementation complete, pending testing.        |
| Tested      | All tests pass, pending final acceptance.        |
| Done        | Fully implemented, tested, and accepted.         |
| Obsolete    | No longer valid, superseded by another use case. |

### Note

FHHS also exposes `POST /pedigree/family`, a debug/introspection endpoint that
returns the assembled family data as plain text without rendering a diagram.
It supports step 2 above during troubleshooting and is not modeled as a
separate use case.
