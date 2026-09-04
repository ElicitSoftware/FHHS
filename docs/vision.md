# Family Health History Survey (FHHS) — Vision

> **Draft.** This is the AIUP seed document. Edit freely — `/requirements`
> and the rest of the AIUP workflow read from here.

## Mission

FHHS is the report-generation backend for the Family Health History Survey:
an adaptable survey, developed by Michigan Medicine for the Michigan Genetic
Hereditary Testing (MiGHT) Study, that assesses an individual's risk of
hereditary cancer syndromes from their personal and family cancer history.
FHHS itself renders no UI — it is a headless Quarkus service that the rest of
the [Elicit Software](https://github.com/ElicitSoftware/) platform (the
respondent-facing **Survey** app and the **Admin** console) calls into as a
**post-survey action** once a respondent finishes the questionnaire. FHHS
turns the recorded answers about the participant and their first- and
second-degree biological relatives (children, siblings, parents,
aunts/uncles, grandparents) into a personal and family cancer report with a
pedigree diagram, downloadable by both participants and clinicians.

## Target Users / Actors

FHHS has no human users of its own — its "actors" are the systems and
schedules that drive it:

- **Survey Platform** — the Elicit Survey application. On respondent
  finalization it consults `post_survey_actions` and makes authenticated HTTP
  POST calls into FHHS's report endpoints (`/proband/report`,
  `/casummary/report`, `/pedigree/report`, `/pedigree/family`,
  `/familyhistory/generate`). This is FHHS's primary actor.
- **Scheduler** — FHHS's own internal `@Scheduled` job, which periodically
  drains a queue of pending `RespondentPSA`/`Status` rows and generates and
  delivers (via SFTP) any family-history reports that haven't completed yet.
- **Pedigree Drawing Service** — an external R service (using the
  `kinship2` package) that FHHS calls to render the pedigree diagram
  embedded in the family/cancer reports.
- **Clinician / Participant** — the ultimate readers of the generated PDF
  reports, but they never call FHHS directly; they receive the PDF via the
  Survey/Admin UI or the SFTP-delivered file.

## Goals

- Generate a personal cancer-risk ("proband") report from a respondent's own
  answers.
- Generate a cancer-risk summary report.
- Generate a visual pedigree (via the external Kinship2-based service) from
  the respondent's reported family structure and cancer history.
- Generate a combined family-history PDF report (member summary, per-member
  detail, cancer-history counts by type) and deliver it, plus an XML metadata
  sidecar, to an SFTP destination.
- Process family-history report generation asynchronously so it never blocks
  survey completion, and retry/recover pending work via a scheduled sweep of
  `post_survey_actions` status.
- Operate as a stateless-per-request Quarkus service, backed by the shared
  Elicit PostgreSQL database, deployable via Docker alongside Survey/Admin.

## Scope

**In scope:**
- REST endpoints that accept a respondent/report request and return or deliver
  a generated PDF (proband, cancer-summary, pedigree, family-history).
- Reading respondent, family-member, and cancer-history data already recorded
  by the Survey application.
- Calling the external pedigree-drawing service and incorporating its output.
- SFTP delivery of the family-history report package.
- Scheduled retry/reconciliation of family-history report generation status.

**Out of scope:**
- Presenting the survey questionnaire itself (owned by Survey).
- Respondent/subject registration, invitations, and progress monitoring
  (owned by Admin).
- Authoring survey content (owned by the Authoring tool).
- Any end-user-facing UI — FHHS is API/backend only.

## Non-Functional Priorities (see `docs/requirements.md` for measurable NFRs)

- **Security**: report endpoints that expose PHI-adjacent cancer/family
  history data must be authenticated (role-based — e.g. `proband-user`,
  `cssummary-user`); endpoints currently running `@PermitAll`
  (`/pedigree/*`, `/familyhistory/*`) are a known gap to close.
- **Reliability**: family-history generation must not be lost if it fails
  partway — the scheduled sweep exists specifically to recover pending work.
- **Traceability**: every report generated must be attributable to a
  respondent and externally-issued study ID.
