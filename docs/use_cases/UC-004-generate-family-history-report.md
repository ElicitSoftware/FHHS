# Use Case: Generate Family History Report

## Overview

**Use Case ID:** UC-004
**Use Case Name:** Generate Family History Report
**Primary Actor:** Survey Platform
**Secondary Actor:** Scheduler
**Goal:** Produce the full family-history PDF report and metadata file for a finalized respondent, and deliver both to the configured SFTP destination, without making the respondent wait for delivery.
**Status:** Implemented

## Preconditions

- The respondent has finalized the survey (`finalizedDt` is set).
- A `POST_SURVEY_ACTION` for family history report generation is configured on the respondent's survey.
- SFTP delivery is enabled in configuration (BR-003); otherwise this use case does not run.

## Main Success Scenario

1. The Survey Platform notifies FHHS that a respondent has finalized the survey, providing the respondent ID and external (study) ID.
2. The system immediately acknowledges the request as accepted (BR-001) and continues the remaining steps in the background.
3. The system generates a PDF report covering the family member summary, per-member detail, and cancer-history counts by type.
4. The system generates an XML metadata file describing the respondent, external ID, generation date, and produced files.
5. The system uploads the PDF and XML files to the configured SFTP destination.
6. The system records the execution as successful, including the upload timestamp.

## Alternative Flows

### A1: Generation or Upload Fails

**Trigger:** PDF generation or the SFTP upload throws an error (step 3).
**Flow:**

1. The system records the execution as failed, including the error message and incrementing the try count. Processing returns to the Scheduler for retry (see A2). Use case ends.

### A2: Scheduler Retries Pending Work

**Trigger:** The Scheduler's periodic sweep runs (every 15 minutes) and finds respondent execution records that have not reached a final successful status (BR-002) (step 3).
**Flow:**

1. The Scheduler re-attempts PDF generation and SFTP upload for each pending record. Use case continues at step 3.

### A3: SFTP Delivery Disabled

**Trigger:** SFTP delivery is disabled in configuration (BR-003) (step 1).
**Flow:**

1. The system skips report generation, upload, and the scheduled retry sweep entirely for this respondent. Use case ends.

## Postconditions

### Success Postconditions

- The PDF and XML files exist at the configured SFTP destination.
- The respondent's execution record reflects success and an upload timestamp.

### Failure Postconditions

- The respondent's execution record reflects failure, an error message, and an incremented try count, and remains eligible for the next scheduled retry.

## Business Rules

### BR-001: Non-Blocking Acknowledgment

The system must acknowledge a family-history report request immediately and perform generation and delivery asynchronously, so the triggering survey-completion flow is never blocked waiting on it.

### BR-002: Scheduled Retry

Any family-history report execution that has not reached a final successful status is retried by the scheduled sweep, which runs every 15 minutes.

### BR-003: SFTP Delivery Can Be Disabled

SFTP delivery can be disabled entirely via configuration; when disabled, no connection test, generation, upload, or retry occurs for any respondent.

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
