# FHHS — Requirements

Derived from `docs/vision.md`. FHHS has no human users; the "role" in each
functional requirement below is one of its system actors (see
`docs/vision.md` → Target Users / Actors and `docs/use_cases.puml`).

## Functional Requirements (FR)

| ID     | Title                                | User Story                                                                                                                                              | Priority | Status      |
|--------|---------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-------------|
| FR-001 | Generate Proband Report               | As a Survey Platform, I want to request a personal cancer-risk report for a respondent so that the participant receives their own risk assessment.    | High     | Implemented |
| FR-002 | Generate Cancer Summary Report        | As a Survey Platform, I want to request a cancer-risk summary report for a respondent so that clinicians get a condensed risk overview.                | High     | Implemented |
| FR-003 | Generate Pedigree Report              | As a Survey Platform, I want to request a pedigree diagram report for a respondent so that the family structure and cancer history are shown visually. | High     | Implemented |
| FR-004 | Generate Family History Report        | As a Survey Platform, I want to trigger generation and delivery of the full family-history report so that the study site receives it without the participant waiting on it. | High     | Implemented |
| FR-005 | Retry Pending Family History Reports  | As a Scheduler, I want to periodically re-attempt family-history reports that haven't completed so that transient failures don't lose a report permanently. | High     | Implemented |

## Non-Functional Requirements (NFR)

| ID      | Title                          | Requirement                                                                                                                            | Category        | Priority | Status |
|---------|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|-----------------|----------|--------|
| NFR-001 | Report Endpoint Authorization   | 100% of endpoints that return respondent cancer/family-history data must require a role-based credential (e.g. `proband-user`, `cssummary-user`). `/familyhistory/generate` is explicitly `@PermitAll` and `/pedigree/report` carries no security annotation at all (equally unauthenticated); neither meets this yet. | Security        | High     | Open   |
| NFR-002 | Non-Blocking Report Generation | The `/familyhistory/generate` HTTP call must return in under 2 seconds; the PDF/XML generation and SFTP upload it triggers must run asynchronously in the background. | Performance     | High     | Verified |
| NFR-003 | Pending Report Recovery        | Any family-history report execution left in `FAILED` status (not yet uploaded, and with fewer than 50 tries) must be retried by the scheduled sweep, which runs every 15 minutes. A report stuck in `STARTED` (e.g. the process crashed mid-generation) is not picked up by the sweep. | Availability    | High     | Verified |
| NFR-004 | External Call Observability    | 100% of calls to the external pedigree-drawing service must be wrapped in an OpenTelemetry client span recording status code and latency. | Maintainability | Medium   | Verified |
| NFR-005 | Pedigree Service Timeout       | Calls to the external pedigree-drawing service must time out and fail gracefully (report still generated, with a placeholder) after 15 seconds. | Availability    | Medium   | Verified |
| NFR-006 | Report Traceability            | 100% of family-history report execution records must carry a non-null respondent ID and externally-issued study ID (XID), so every generated report can be traced back to the respondent and study it was produced for. | Maintainability | High     | Open |

## Constraints (C)

| ID    | Title                     | Constraint                                                                                          | Category    | Priority | Status |
|-------|----------------------------|------------------------------------------------------------------------------------------------------|-------------|----------|--------|
| C-001 | Runtime Platform          | Backend must run on Java 25 (`maven.compiler.release=25`).                                           | Technical   | High     | Verified |
| C-002 | Application Framework     | Backend must be built on Quarkus 3.37.x.                                                             | Technical   | High     | Verified |
| C-003 | Database Platform         | System must use PostgreSQL, sharing the `survey` schema owned by the Elicit Survey application.      | Technical   | High     | Verified |
| C-004 | Data Access Layer         | Data access must use Hibernate ORM with Panache (active-record entities) — not jOOQ.                 | Technical   | High     | Verified |
| C-005 | External Pedigree Service | Pedigree diagram rendering requires an external R/Kinship2-based HTTP service reachable at the configured `pedigree.url`. | Technical   | High     | Verified |
| C-006 | Deployment                | Must be deployable as a Docker container alongside Survey, Admin, and PostgreSQL via `docker-compose`. | Operational | High     | Verified |
