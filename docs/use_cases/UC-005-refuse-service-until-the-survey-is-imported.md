# Use Case: Refuse Service Until the Survey Is Imported

## Overview

**Use Case ID:** UC-005
**Use Case Name:** Refuse Service Until the Survey Is Imported
**Primary Actor:** Deployment Operator
**Secondary Actor:** Survey Platform
**Goal:** Start against a database that does not yet contain the Family History Survey, say so in one place an operator will read, and become ready by itself once the survey has been imported, without a restart.
**Status:** Implemented

## Preconditions

- The database schema exists (Survey has created it) and FHHS's own migrations have run. None of them creates the survey any more: a deployment imports `FHHS/family-history-survey.elicit` through Admin's Apply Survey Definition screen.
- FHHS is configured with the key of the survey it serves (`family.history.survey.key`).

## Main Success Scenario

1. The operator starts FHHS on a database with no Family History Survey.
2. The system starts, finds no survey with the configured key, and writes one warning to the service log naming the key and the import to perform.
3. The system reports not-ready on its readiness probe, carrying the same instruction, so the container stays unhealthy rather than exited.
4. While the survey is absent, the system refuses every report request with 503 and the same instruction; the health and debug endpoints still answer.
5. The operator imports the survey definition through Admin.
6. On its next readiness probe the system finds the survey, reports ready, and serves reports from then on. No restart was needed.

## Alternative Flows

### A1: A different survey is present

**Trigger:** The database holds a survey, but not one with the configured key (step 2).
**Flow:**

1. The system treats the survey as absent: it is specific to the Family History Survey's steps and questions, and would fail part-way through any report otherwise (BR-001).
2. Use case continues at step 2.

### A2: The survey was already imported

**Trigger:** A survey with the configured key exists when the system starts (step 2), as on every upgraded deployment, where the earlier migrations seeded it.
**Flow:**

1. The system logs nothing, reports ready, and serves reports.
2. Use case ends.

## Postconditions

### Success Postconditions

- The system is ready and serving reports, with the imported survey's own ids in use: the post-survey action FHHS records its uploads against is found by its key, not assumed to be id 1 (BR-004).

### Failure Postconditions

- The system stays not-ready and refuses reports, and the log and the readiness probe both say why.

## Business Rules

### BR-001: The survey is recognised by its key

"Any survey exists" is not the test. FHHS switches on the Family History Survey's step names and reads reporting columns generated from its question set, so only a survey with the configured key counts. The key is fixed across sites and revisions and travels with the definition file.

### BR-002: Absence is re-checked, presence is remembered

While the survey is absent, every readiness probe and every request re-checks, so the import is noticed at once. Once found, the answer is kept: an imported survey is not removed.

### BR-003: Refusal explains

A refused request answers 503 with the same instruction the log and the readiness probe carry. The endpoints that exist to say what is wrong -- the health endpoint and the debug endpoint -- are never refused.

### BR-004: The post-survey action is found by its key

Its id is whatever the import minted on this site, so `family.history.upload.psa.key` names it and the id is looked up. `family.history.upload.psa.id`, if set, still pins an explicit id.

### BR-005: Nothing on the greenfield migration track needs the survey

The migrations that seeded the survey, its report definitions and its post-survey action are gone from the greenfield track; the definition file carries all three. What remains (grants, indexes on the star schema, sequence hygiene) runs on an empty database. The frozen upgrade track for pre-V3 databases still seeds, and those databases keep their survey. A database that once applied the removed versions still validates cleanly against the greenfield track, because a recorded version the track no longer carries is tolerated rather than read as an un-upgraded history.

---

## Reference

Traces to FR-006 and C-008. Implemented by `FamilyHistorySurveyCheck` (the check, the startup
warning and the instruction), `FamilyHistorySurveyHealthCheck` (readiness),
`FamilyHistorySurveyRequiredFilter` (the 503), the `surveyKey` and `postSurveyActionKey`
columns on `Survey` and `PostSurveyAction`, `FamilyHistoryReportService.psaId()` (BR-004) and the
`*:missing` tolerance in `ManualSchemaMigrator` (BR-005). The former seeds live on as the test
fixture under `src/test/resources/db/test`, so the tests run against the real survey structure.
Verified by `FamilyHistorySurveyCheckTest`, `FamilyHistorySurveyRequiredFilterTest`,
`PostSurveyActionResolutionTest` and `ManualSchemaMigratorUpgradeTest`.

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
