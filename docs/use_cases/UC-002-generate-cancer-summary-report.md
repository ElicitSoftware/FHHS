# Use Case: Generate Cancer Summary Report

## Overview

**Use Case ID:** UC-002
**Use Case Name:** Generate Cancer Summary Report
**Primary Actor:** Survey Platform
**Goal:** Obtain a condensed PDF report of a respondent's cancer-risk-relevant history for clinician review.
**Status:** Implemented

## Preconditions

- The respondent has a record in FHHS's shared database and has recorded family history answers.
- The calling system presents the platform service token (UC-006).

## Main Success Scenario

1. The Survey Platform requests a cancer summary report for a respondent, identified by respondent ID.
2. The system retrieves the respondent's and family members' cancer diagnosis history.
3. The system summarizes the diagnoses relevant to cancer-risk assessment.
4. The system renders the summary as both an HTML fragment and a PDF document.
5. The system returns the report title, HTML content, and PDF document to the caller.

## Alternative Flows

### A1: No Relatives Reported Cancer History

**Trigger:** Summarizing the retrieved history produces no cancer-relevant diagnosis rows for any family member (step 3).
**Flow:**

1. The system returns a report stating that no relatives reported cancer, rather than failing the request. Use case ends.

### A2: Caller Is Not Authenticated

**Trigger:** The request carries no service token, or a wrong one (UC-006 A1, A2) (step 1).
**Flow:**

1. The system answers 401 and reads no respondent data. Use case ends.

### A3: Request Has No Respondent ID

**Trigger:** The request has no body, or its respondent ID is missing or not a positive number (step 1).
**Flow:**

1. The system answers 400 and reads no respondent data (NFR-010). Use case ends.

### A4: Respondent Is Not of the Family History Survey

**Trigger:** The respondent ID is that of a respondent of another survey, or of no respondent (UC-006 A8) (step 1).
**Flow:**

1. The system answers 404 with a fixed sentence and reads no report data. Use case ends.

## Postconditions

### Success Postconditions

- A cancer summary report (HTML + PDF) reflecting the respondent's recorded cancer history is returned to the caller.

### Failure Postconditions

- No report is generated; the caller receives an error status.

## Business Rules

### BR-001: Only an Authenticated Service May Ask

Only a caller presenting the platform service token may request a cancer summary report (UC-006). FHHS has no roles: the token says that a platform module is calling, and the decision on whose behalf was made by that module.

### BR-002: Report HTML Carries Text, Not Markup, From Answers

The HTML fragment is built from markup FHHS writes itself. Everything it places inside that markup from the survey -- answer text, including the name of an "other" cancer, and step names -- is HTML-encoded (NFR-011), so it is shown as typed and never interpreted. The PDF table carries the same text unencoded.

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
