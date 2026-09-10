# Use Case: Generate Cancer Summary Report

## Overview

**Use Case ID:** UC-002
**Use Case Name:** Generate Cancer Summary Report
**Primary Actor:** Survey Platform
**Goal:** Obtain a condensed PDF report of a respondent's cancer-risk-relevant history for clinician review.
**Status:** Implemented

## Preconditions

- The respondent has a record in FHHS's shared database and has recorded family history answers.
- The calling system presents a credential with the `cssummary-user` role (BR-001).

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

### A2: Caller Lacks the Required Role

**Trigger:** The caller's credential does not include the `cssummary-user` role (BR-001) (step 1).
**Flow:**

1. The system rejects the request as unauthorized. Use case ends.

## Postconditions

### Success Postconditions

- A cancer summary report (HTML + PDF) reflecting the respondent's recorded cancer history is returned to the caller.

### Failure Postconditions

- No report is generated; the caller receives an authorization error.

## Business Rules

### BR-001: Cancer Summary Report Authorization

Only callers presenting a credential with the `cssummary-user` role may request a cancer summary report for a respondent.

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
