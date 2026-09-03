# Use Case: Generate Proband Report

## Overview

**Use Case ID:** UC-001
**Use Case Name:** Generate Proband Report
**Primary Actor:** Survey Platform
**Goal:** Obtain a PDF report summarizing a respondent's own family-history health survey answers.
**Status:** Implemented

## Preconditions

- The respondent has a record in FHHS's shared database and has recorded family history answers.
- The calling system presents a credential with the `proband-user` role (BR-001).

## Main Success Scenario

1. The Survey Platform requests a proband report for a respondent, identified by respondent ID.
2. The system retrieves all family history records recorded for that respondent.
3. The system assembles the records into a summary table.
4. The system renders the summary as both an HTML fragment and a PDF document.
5. The system returns the report title, HTML content, and PDF document to the caller.

## Alternative Flows

### A1: Respondent Has No Recorded Family History

**Trigger:** The respondent has no family history records.
**Flow:**

1. The system returns a report containing an empty summary table rather than failing the request.

### A2: Caller Lacks the Required Role

**Trigger:** The caller's credential does not include the `proband-user` role (BR-001).
**Flow:**

1. The system rejects the request as unauthorized.

## Postconditions

### Success Postconditions

- A proband report (HTML + PDF) reflecting the respondent's recorded family history is returned to the caller.

### Failure Postconditions

- No report is generated; the caller receives an authorization error.

## Business Rules

### BR-001: Proband Report Authorization

Only callers presenting a credential with the `proband-user` role may request a proband report for a respondent.

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
