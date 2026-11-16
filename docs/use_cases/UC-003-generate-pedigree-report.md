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

### A2: Request Has No Respondent ID

**Trigger:** The request has no body, or its respondent ID is missing or not a positive number (step 1).
**Flow:**

1. The system answers 400 and reads no respondent data (NFR-010). Use case ends.

## Postconditions

### Success Postconditions

- A pedigree report (diagram + PDF) reflecting the respondent's family structure and cancer history is returned to the caller.

### Failure Postconditions

- A pedigree report is still returned, but with a placeholder in place of the diagram (see A1) — the use case has no hard failure postcondition.

## Business Rules

### BR-001: Pedigree Diagram Degrades Gracefully

If the external Pedigree Drawing Service fails, times out, or returns invalid diagram content, the report must still be generated, showing a placeholder message instead of the diagram rather than failing the request. The placeholder is the fixed sentence "Pedigree diagram unavailable." in the HTML and in the PDF; the cause (the service's status code or the connection error) is written to the log and the trace, never into the report (NFR-010).

### BR-002: The Family Table Is Sent, Not Recorded

The family table sent to the Pedigree Drawing Service is written to no log or trace. The log records how many rows and characters were sent and the status code the service answered with (NFR-009).

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
