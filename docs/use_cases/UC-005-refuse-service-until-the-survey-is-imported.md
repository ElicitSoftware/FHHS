# Use Case: Refuse Service Until the Survey Is Imported

## Overview

**Use Case ID:** UC-005
**Use Case Name:** Refuse Service Until the Survey Is Imported
**Primary Actor:** Deployment Operator
**Secondary Actor:** Survey Platform
**Goal:** Start against a database that does not yet contain the Family History Survey, or holds it but has not yet built its reporting schema, say so in one place an operator will read, and become ready by itself once the survey has been imported and built, without a restart.
**Status:** Implemented

## Preconditions

- The database schema exists (Survey has created it) and FHHS's own migrations have run. None of them creates the survey any more: a deployment imports `FHHS/family-history-survey.elicit` through Admin's Apply Survey Definition screen.
- FHHS is configured with the key of the survey it serves (`family.history.survey.key`).
- Reports read the survey's own reporting schema, the one Survey creates for it on its first build and names on `survey.surveys.report_schema` (Survey UC-008 BR-006). Admin's Apply Survey Definition asks Survey for that build as part of the apply.

## Main Success Scenario

1. The operator starts FHHS on a database with no Family History Survey.
2. The system starts, finds no survey with the configured key, and writes one warning to the service log naming the key and the import to perform.
3. The system reports not-ready on its readiness probe, carrying the same instruction, so the container stays unhealthy rather than exited.
4. While the survey is absent, the system refuses every report request with 503 and the same instruction; the health and debug endpoints still answer.
5. The operator imports the survey definition through Admin, whose apply also has Survey build the survey's reporting schema.
6. On its next readiness probe the system finds the survey with its reporting schema named, reports ready, and serves reports from then on, each report resolving the schema's name at the time it runs (BR-006). No restart was needed.

## Alternative Flows

### A1: A different survey is present

**Trigger:** The database holds a survey, but not one with the configured key (step 2).
**Flow:**

1. The system treats the survey as absent: it is specific to the Family History Survey's steps and questions, and would fail part-way through any report otherwise (BR-001).
2. Use case continues at step 2.

### A2: The survey was already imported and built

**Trigger:** A survey with the configured key exists and has a reporting schema when the system starts (step 2), as on every upgraded deployment once Survey's first start after the upgrade has regenerated it.
**Flow:**

1. The system logs nothing, reports ready, and serves reports.
2. Use case ends.

### A3: The survey is imported but its reporting schema has not been built

**Trigger:** A survey with the configured key exists but `report_schema` is null (step 2): the import landed and Survey's build has not run yet, failed, or the schema was dropped.
**Flow:**

1. The system treats the survey as not ready: there is no schema to read reports from. It writes one warning naming the key and the build to perform (Admin's apply builds it; so does `POST /api/etl/build?survey=<key>` on Survey), reports not-ready carrying the same instruction, and refuses report requests with 503 and that instruction (BR-003).
2. On every readiness probe and request the schema name is re-read (BR-002), so the build is noticed at once.
3. Use case continues at step 6.

## Postconditions

### Success Postconditions

- The system is ready and serving reports, with the imported survey's own ids in use: the post-survey action FHHS records its uploads against is found by its key, not assumed to be id 1 (BR-004).

### Failure Postconditions

- The system stays not-ready and refuses reports, and the log and the readiness probe both say why.

## Business Rules

### BR-001: The survey is recognized by its key

"Any survey exists" is not the test. FHHS switches on the Family History Survey's step names and reads reporting columns generated from its question set, so only a survey with the configured key counts. The key is fixed across sites and revisions and travels with the definition file.

### BR-002: Absence is re-checked, presence is remembered

While the survey is absent, every readiness probe and every request re-checks, so the import is noticed at once. Once found, the answer is kept: an imported survey is not removed. The reporting schema's name, by contrast, is never kept (BR-006).

### BR-003: Refusal explains

A refused request answers 503 with the same instruction the log and the readiness probe carry. The endpoints that exist to say what is wrong -- the health endpoint and the debug endpoint -- are never refused.

### BR-004: The post-survey action is found by its key

Its id is whatever the import minted on this site, so `family.history.upload.psa.key` names it and the id is looked up. `family.history.upload.psa.id`, if set, still pins an explicit id.

### BR-006: The reporting schema is resolved by the survey key, every time

Every survey at a site reports in a schema of its own, whose name a site derives and may change (Survey UC-008 BR-006, UC-010). FHHS never hard-codes it: each report looks the name up from `survey.surveys.report_schema` by `family.history.survey.key` at the moment it runs, and the readiness check does the same, so a rename takes effect on the next request and a drop is noticed as "not built". The name is validated as an unquoted identifier before it is spliced into the query, because a schema name cannot be bound as a parameter. The columns FHHS selects from that schema's `fact_sections_view` are unchanged by the move.

### BR-005: Nothing on the greenfield migration track needs the survey

The migrations that seeded the survey, its report definitions and its post-survey action are gone from the greenfield track; the definition file carries all three. What remains (grants, sequence hygiene) runs on an empty database; the indexes on the star schema are Survey's, created per survey schema by its ETL. The frozen upgrade track for pre-V3 databases still seeds, and those databases keep their survey. A database that once applied the removed versions still validates cleanly against the greenfield track, because a recorded version the track no longer carries is tolerated rather than read as an un-upgraded history.

---

## Reference

Traces to FR-006 and C-008. Implemented by `FamilyHistorySurveyCheck` (the check, the startup
warning, both instructions and the per-call schema lookup of BR-006), `FamilyHistorySurveyHealthCheck`
(readiness), `FamilyHistorySurveyRequiredFilter` (the 503), the `surveyKey`, `reportSchema` and
`postSurveyActionKey` columns on `Survey` and `PostSurveyAction`, `CancerHistoryRepository` (the
resolved qualifier), `FamilyHistoryReportService.psaId()` (BR-004) and the `*:missing` tolerance in
`ManualSchemaMigrator` (BR-005). The former seeds live on as the test
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
