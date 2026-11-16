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
- The Survey Platform presents the platform service token (UC-006). The Scheduler is internal and presents nothing to start a retry.

## Main Success Scenario

1. The Survey Platform notifies FHHS that a respondent has finalized the survey, providing the respondent ID and external (study) ID.
2. The system immediately acknowledges the request as accepted (BR-001) and continues the remaining steps in the background.
3. The system generates a combined PDF report by invoking each report configured for the respondent's survey and assembling their content into a single document (BR-004).
4. The system generates an XML metadata file describing the respondent, external ID, generation date, and produced files.
5. The system uploads the PDF and XML files to the configured SFTP destination, after verifying the server's host key (BR-006).
6. The system records the execution as successful, including the upload timestamp.

## Alternative Flows

### A1: Generation or Upload Fails

**Trigger:** PDF generation or the SFTP upload throws an error, or the respondent's XID cannot name the delivery files (BR-007) (step 3).
**Flow:**

1. The system records the execution as failed, including the error message and incrementing the try count. Processing returns to the Scheduler for retry (see A2). Use case ends.

### A2: Scheduler Retries Pending Work

**Trigger:** The Scheduler's periodic sweep runs (every 15 minutes) and finds respondent execution records with `FAILED` status, not yet uploaded, with fewer than 50 tries so far (BR-002) (step 3).
**Flow:**

1. The Scheduler re-attempts PDF generation and SFTP upload for each such record. Use case continues at step 3.

### A3: SFTP Delivery Disabled

**Trigger:** SFTP delivery is disabled in configuration (BR-003) (step 1).
**Flow:**

1. The system skips report generation, upload, and the scheduled retry sweep entirely for this respondent. Use case ends.

### A4: Request Has No Respondent ID

**Trigger:** The request has no body, or its respondent ID is missing or not a positive number (step 1).
**Flow:**

1. The system answers 400 and starts nothing (NFR-010). Use case ends.

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

Any family-history report execution left in `FAILED` status (not yet uploaded, with fewer than 50 tries so far) is retried by the scheduled sweep, which runs every 15 minutes. An execution stuck in `STARTED` — for example because the process crashed mid-generation before it could be marked `FAILED` — is not retried.

### BR-003: SFTP Delivery Can Be Disabled

SFTP delivery is disabled unless configuration enables it; while disabled, no connection test, generation, upload, or retry occurs for any respondent. A deployment turns it on once it has an SFTP target to deliver to.

### BR-004: Report Composition Is Configuration-Driven

The set of reports assembled into the combined PDF is determined by the survey's configured report list, not hardcoded in FHHS. The default configuration composes the Proband (UC-001), Cancer Summary (UC-002), and Pedigree (UC-003) reports, in that order. Each report is invoked at the address its definition carries. FHHS presents the service token on that call only when the address's origin is one the deployment trusts (`elicit.service.trusted-origins`, UC-006 BR-006), follows no redirect, and with no token configured does not make the call; the report's section then carries the error, as for any report that could not be obtained.

### BR-005: XML Metadata Placeholders

The XML metadata file is built from the configurable `family.history.sftp.xml.template`. The template may use `{RespondentId}`, `{Xid}`, `{SurveyId}`, `{FirstName}`, `{LastName}`, `{MiddleName}`, `{Dob}`, `{Email}`, `{Phone}`, `{DepartmentName}`, `{DepartmentID}`, `{AccessCode}` (the respondent's survey access code), `{Status}`, `{Created}`, and `{Finalized}`. A missing value is substituted as empty text, and any other placeholder, including the former `{Token}`, is left unchanged. The template is filled in a single pass: text that arrives in a value is placed as it is and is never read as a placeholder, so a value that happens to contain `{AccessCode}` stays that literal text. For a value that contains no placeholder text the file is byte for byte what sequential substitution produced.

### BR-006: Delivery Only to a Verified Server

The report carries PHI, so it is handed only to a server that proves it holds a host key the deployment configured (`family.history.sftp.knownHosts`: known_hosts lines from `ssh-keyscan`, or a known_hosts file). With no key configured, or a server presenting a different one, the connection is refused and the execution fails (A1), to be retried once the configuration is corrected (A2). `family.history.sftp.strictHostKeyChecking=false` skips the check, for the bundled test SFTP server whose key changes on every start; FHHS logs a warning on each such connection (NFR-007).

### BR-007: File Names Come From a Usable XID

The PDF is `<XID>.pdf` and the metadata file `<XID>-index.xml`, written in the configured directory. An XID that is missing or blank, is `.` or `..`, or contains `/`, `\` or a control character cannot name a file: the execution fails before anything is generated or uploaded (A1), and the recorded error and the log never carry the value. Correcting the XID in Admin lets the next sweep deliver it (A2). Any other XID names the files exactly as before (NFR-012). Two respondents with the same XID are the same subject to the receiving system; the later delivery replaces the earlier file.

### BR-008: Delivery Is Logged by Respondent ID

Progress and failure records name the respondent ID, the kind of file (PDF, XML metadata) and its size. They never carry the file name, the XID or the metadata content (NFR-009). An operator who needs the XID looks it up by respondent ID in `survey.respondents`.

### BR-009: A Failed Request Explains Without Internals

When the request cannot be started, the caller gets 500 with a fixed sentence; the cause is logged with the respondent ID (NFR-010).

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
