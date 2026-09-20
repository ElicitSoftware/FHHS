# Kimball Type 2 Impact on FHHS

> **Status (2026-09-15):** Survey has shipped Kimball Type 2 (`Survey`'s
> `V010__Kimball_Type2_SCD.sql` / greenfield `V001__Create_Survey_Schema.sql`), and FHHS's
> side of the impact described below is now **implemented**: `V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql`
> replaces the surrogate-id UPDATE in `V0.0.5__UPDATE_CANCER_QUESTONS.sql`. See section 2 and
> section 9 for what changed. (Earlier note, 2026-09-04: this document was revised to drop
> content that referred to `surveyreport.FACT_FHHS_VIEW` and `FactFHHSView.java` — both were
> removed from FHHS in `V0.0.7__DROP_FHHS_FACT_VIEW.sql` (commit `548202c`, "Removed
> FHHS_FACT_VIEW and optimized queries", 2026-03-11), which predates the original version of
> this document. The risk that section was written to address no longer exists in the current
> codebase.)
>
> **Update (2026-09-15):** A genuinely fresh (greenfield) install against Survey's Kimball
> schema was tested for the first time and found `V0.0.1__POPULATE_FHHS_DATA.sql` broken —
> its `survey.metadata`/`survey.relationships`/`survey.select_items` inserts used pre-Kimball
> column names (`step_section_id`, `section_question_id`, `downstream_s_id`, `group_id`), which
> don't exist on a fresh Kimball database (only the ALTER-based brownfield upgrade path keeps
> them, renamed to `*_surrogate`). A second, subtler issue: `steps`/`sections`/`questions`/
> `select_groups` gained new durable-key columns defaulting via `nextval()` on their own
> sequence — every other table's hardcoded numeric FK literals in `V0.0.1` implicitly assumed
> that durable id equals surrogate id, which only holds if nothing skips a sequence value
> (fixed by explicitly setting the durable column via `currval()` alongside each surrogate
> `nextval()`, and by literal `0` for the one hardcoded `sections` row). This was invisible in
> every prior test because those tests only ever exercised the upgrade path (an existing v2.x
> database), never a from-scratch install. Fixed by porting Survey's dual Flyway migration
> track (`ManualSchemaMigrator`, `db/migration` vs `db/migration-v3`) to FHHS — see section 6.
>
> A third, unrelated bug surfaced and was fixed in the same pass: `ManualSchemaMigrator`'s
> greenfield/brownfield routing check used a plain `Flyway.validate()` call, which treats
> *pending* (not-yet-applied) migrations as a validation failure just like a real checksum
> mismatch. Since `V0.0.3__CREATE_FHHS_FACT_VIEW.sql` legitimately fails on a truly fresh
> install until Survey's reporting ETL has run at least once (a pre-existing, non-Kimball
> characteristic — see `DeploymentScript.md`'s documented "start, restart, restart" fresh-install
> sequence), a database that hits that failure and reboots would be misdiagnosed as an
> unupgraded v2.x database and incorrectly routed to `db/migration-v3` (the legacy-column-name
> track), failing for real. Fixed with `.ignoreMigrationPatterns("*:pending")` on the Flyway
> configuration `ManualSchemaMigrator` uses for its "is this clean" probe. Confirmed end-to-end:
> a full `docker compose up -d && restart && restart` fresh install now succeeds completely —
> all of Survey, Admin, and FHHS report healthy with every migration applied.
>
> **Update (2026-09-19):** The greenfield track (`db/migration`, still unreleased) was found
> broken again once Survey's `V015` made the eight Type 2 `*_key` columns `NOT NULL` with no
> default: `V0.0.1` supplied none, and — because Postgres sequences are not transactional —
> its first failed attempt left every `nextval()`-driven id shifted, so the retry then failed
> earlier still on `ontology.dimension = 1`. `V0.0.1`/`V0.0.2` were rewritten with literal ids
> and fixed UUIDv5 keys (derived under the `survey_key` namespace from `<table>:<id>`), the
> Cancers-section reorder was folded into the seed, and `V0.0.3`, `V0.0.5` and `V0.0.8` became
> documented no-ops on this track (kept so `repair()` still lines the versions up with
> `db/migration-v3`). Since `V0.0.3` no longer needs the ETL-built `fact_sections_view`, a
> fresh install is now a single `docker compose up -d` — no restart passes.

## Overview

FHHS interacts with the survey database in two ways:

1. **Runtime reads** — FHHS services (`casummary`, `pedigree`, `proband`, `familyhistory`)
   read directly from `surveyreport.fact_sections_view` via
   `CancerHistoryRepository.findFamilyHistoryByRespondentId()`. This view is built on top of
   the `dim_step` / `dim_section` dimension tables and dynamically-added per-ontology-tag
   columns (`bladder_cancer`, `breast_cancer`, etc.). No direct queries against the raw
   `survey.*` structural tables are made at runtime, and no query hardcodes a `step_key` or
   `section_key` surrogate value.

2. **One-time Flyway writes** — One FHHS migration directly INSERTs or UPDATEs rows in
   `survey.*` structural tables:
   - `V0.0.5__UPDATE_CANCER_QUESTONS.sql` — updates `survey.sections_questions`
     `display_order` values using hardcoded surrogate `section_id` and row `id` values.

After Survey implements Kimball Type 2, the second layer is affected. The first is not —
see Section 1 below.

---

## 1. `CancerHistoryRepository` — No Hardcoded Dimension Keys (historical note)

### Why this is not a problem

Earlier revisions of FHHS queried `surveyreport.FACT_FHHS_VIEW`, a view built from a `UNION`
of two queries against `fact_sections_view` that filtered on hardcoded surrogate
`step_key`/`section_key` integer literals (e.g. `f1.section_key = 1`, `f1.step_key != 3`).
That view — and the hardcoded-key risk it carried under a Kimball Type 2 migration — no
longer exists.

`V0.0.7__DROP_FHHS_FACT_VIEW.sql` dropped `FACT_FHHS_VIEW` and the corresponding
`FactFHHSView.java` entity was deleted in the same change. `CancerHistoryRepository`
now queries `surveyreport.fact_sections_view` directly, filtered only by `respondent_id`:

```sql
SELECT f.step, f.step_instance, f.relationship, f.age, f.gender, f.vital_status, ...
       f.bladder_cancer, f.bladder_cancer_age, f.breast_cancer, ...
FROM   surveyreport.fact_sections_view f
WHERE  f.respondent_id = ?1
ORDER  BY f.relationship, f.step
```

The demographic and cancer values are already pivoted into per-ontology-tag columns by the
Survey ETL before FHHS ever reads them. There is no `step_key`/`section_key` literal
anywhere in FHHS's Java code (verified: no matches for `step_key`/`section_key` in
`src/main/java/`). Whatever surrogate ids Survey's Kimball Type 2 migration assigns or
reassigns to `dim_step` / `dim_section` rows, this query is unaffected — it never
references them.

**Required action: none.** No new migration or code change is required in FHHS for this
concern.

---

## 2. `V0.0.5__UPDATE_CANCER_QUESTONS.sql` — Surrogate-Key UPDATE

### Current implementation

```sql
-- Move questions down, making room for the triple negative question
UPDATE survey.sections_questions
SET display_order = display_order + 1
WHERE section_id = 14
  AND display_order > 7;

-- Insert the triple negative question under breast cancer
UPDATE survey.sections_questions
SET display_order = 8
WHERE section_id = 14
  AND id = 125;
```

### Why this migration continues to work on existing deployments

This migration already ran before the Kimball Type 2 migration executes. On an existing
database:

1. V0.0.5 ran: correct `display_order` values are in place.
2. Kimball Type 2 migration runs: it closes existing rows and inserts version-1 rows with
   the updated `display_order` values already set by V0.0.5. The data is consistent.

### Why this migration breaks on fresh deployments

On a clean database:

1. All FHHS Flyway migrations run in order (V0.0.1 through the latest).
2. V0.0.5 fires with `WHERE section_id = 14`. At this point, `sections_questions.section_id`
   still contains the old surrogate FK value (pre-Kimball). If the FHHS migrations run
   **before** Survey's Kimball Flyway migrations, `section_id = 14` is the correct
   surrogate `sections.id` value and the UPDATE works.
3. Survey's Kimball migrations then fire and overwrite `sections_questions.section_id`
   with the durable `sections.section_id` value, which is a **different integer**.

The result is the `display_order` UPDATE in V0.0.5 may or may not match any rows when it
runs after the Kimball migration, depending on deployment order.

### Required fix: new migration using durable keys — **implemented**

`V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql` performs the same reordering logic using
durable keys and the Type 2 versioning protocol (close old row, insert new row). It
resolves the cancer section at migration runtime via `survey.sections.dimension_name =
'Cancers'` and the triple-negative question via `survey.questions.short_text = 'Triple
Negative'` — both confirmed against `V0.0.1__POPULATE_FHHS_DATA.sql`'s seed data (old
surrogate `sections.id = 14` → `dimension_name = 'Cancers'`; old surrogate
`sections_questions.id = 125` → `question_id = 50`, `short_text = 'Triple Negative'`), so
no durable id needs to be looked up or hardcoded ahead of time.

It is also **idempotent**: it first checks whether the triple-negative question's current
`display_order` is already `8`. On an existing deployment (V0.0.5 ran correctly pre-Kimball
and the Kimball migration carried that value forward unchanged), this is already true and
the migration is a no-op for that section. It only performs the shift when V0.0.5's effect
never landed — i.e. exactly the fresh-install/wrong-migration-order case this migration
exists to fix — which also makes it safe to run more than once. See the file itself for the
full PL/pgSQL block.

---

## 3. `dim_step` / `dim_section` — SCD Type 1 Effect on Reports

After Kimball Type 2, the ETL updates `dim_step.value` and `dim_section.value` in-place
whenever a step or section is renamed (SCD Type 1). This means:

- All historical `fact_sections` rows that reference a renamed step or section will now
  display the **new name** in every report.
- The `step` column returned by `CancerHistoryRepository`'s query comes from `dim_step.value`
  (via `fact_sections_view`). If a researcher renames a step (e.g. from `"Sibling"` to
  `"Brother or Sister"`), all existing FHHS reports will display the new label
  retroactively.

This is intentional per the Survey Kimball Type 2 design (`Gap ETL-5`). FHHS reports must
be reviewed after any step or section rename to confirm the new labels are coherent in
context.

**Required action**: Document this behavior for the clinical team. No code change is
required in FHHS, but the clinical team should be notified any time a step or section
name is changed in the Survey Author Tool.

---

## 4. `Respondent.java` — No Changes Required

`Respondent` maps to `survey.respondents` using:

```java
@Column(name = "first_access_dt")
public OffsetDateTime firstAccessDt;
```

`survey.respondents` is not versioned under Kimball Type 2 — it receives no new columns.
`firstAccessDt` is the snapshot anchor, not itself a Type 2 entity. No change needed.

---

## 5. `Survey.java` — No Changes Required

`Survey` maps to `survey.surveys`. Under Kimball Type 2, `surveys` receives only two
new nullable columns (`published_by`, `published_comment`) via SCD Type 1. The Hibernate
mapping does not need to declare these columns unless FHHS ever needs to read/write them.
For FHHS's read-only use of `Survey`, no change is required.

---

## 6. `MigrationService.java` — Superseded by `ManualSchemaMigrator` (revised 2026-09-15)

**Original verdict (no longer correct):** "`MigrationService` handles Flyway programmatic
migrations for FHHS. As long as the new FHHS migration (`V0.0.8`) is placed in the standard
Flyway migration path, `MigrationService` picks it up automatically." That assessment only ever
considered `V0.0.8` (durable-key-safe, one track) — it never audited `V0.0.1`'s `metadata`/
`relationships` inserts, and it didn't anticipate needing a *second*, parallel greenfield track.

`MigrationService` operated on a CDI-injected `Flyway` whose `locations` Quarkus resolves at
**build time** — it has no way to choose between two location sets at runtime. Supporting a real
greenfield/brownfield split (the fix for the `V0.0.1` problem noted in the status banner above)
required replacing it with `com.elicitsoftware.flyway.ManualSchemaMigrator` (ported from the
sibling Survey app), which drives its own independent `Flyway.configure()` instance and picks
`db/migration` (greenfield, fixed column names) vs `db/migration-v3` (frozen, pre-Kimball column
names, preserving checksums for every already-deployed FHHS database) based on `validate()`/
checksum routing at every boot. `MigrationService.java` and its test were deleted;
`quarkus.flyway.owner.migrate-at-start` is now `false` unconditionally.

> **This is temporary scaffolding, not permanent architecture.** `ManualSchemaMigrator.java`,
> `db/migration-v3/`, `src/test/resources/db/test-legacy/`, and
> `ManualSchemaMigratorUpgradeTest.java` exist solely to upgrade already-deployed pre-Kimball
> ("V2.x") FHHS databases to V3. **Once every real FHHS deployment has upgraded** (every
> environment's `flyway_fhhs_history` converged onto `db/migration` — logged by
> `ManualSchemaMigrator` when it happens), all four should be deleted and
> `quarkus.flyway.owner.migrate-at-start` reverted to plain Quarkus-managed auto-migration. See
> the `README.md` inside `db/migration-v3/` and the repo-root `DeploymentScript.md`. **Do not
> let this get pulled into a build forever out of inertia** — track removal as a real follow-up
> once the V2→V3 rollout is confirmed complete.

---

## 7. Post-Survey Action URL — No Changes Required

FHHS registers itself as a post-survey action via `V0.0.4__CREATE_POST_SURVEY_ACTIONS.sql`.
The `survey.post_survey_actions` table is not a structural versioning table and is not
affected by Kimball Type 2.

---

## 8. Implementation Steps (sequenced)

Perform these steps after Survey's Kimball Type 2 Flyway migrations have been verified
in a staging environment:

| Step | Action | File(s) |
|---|---|---|
| 1 | ~~Confirm the durable `section_id` value~~ — not needed. `V0.0.8` resolves the durable section/question ids at migration runtime via `dimension_name = 'Cancers'` / `short_text = 'Triple Negative'`, so there is nothing to look up beforehand. | — |
| 2 | ~~Write `V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql`~~ — **done**. | `src/main/resources/db/migration/V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql` |
| 3 | ~~Deploy FHHS to staging; verify `CancerHistoryRepository.findFamilyHistoryByRespondentId()` returns correct rows for a known respondent~~ — **done** 2026-09-12, against the shared local Postgres (already Kimball-migrated by Survey). `V0.0.8` applied cleanly (recorded in `flyway_fhhs_history`); the cancer section's `display_order` was already correct pre-migration (`Triple Negative` at `8`) and V0.0.8's idempotency check left it untouched (`version = 0`, `published_by` empty on every row) — confirms the "existing deployment" no-op path works against real data, not just the test fixture. `fact_sections_view` queried correctly for a real respondent (id 15). | QA |
| 4 | ~~Verify pedigree and cancer summary reports render correctly for existing respondents~~ — **done** 2026-09-12 for respondent 15 (token `wKckPQH8s`): `/proband/report`, `/casummary/report`, `/pedigree/report` all returned 200 and `POST /familyhistory/generate` produced and SFTP-uploaded a valid 116KB PDF with real content (`Triple Negative Breast Cancer: no`, full pedigree). An initial 400 on the internal report calls was unrelated to Kimball — Quarkus's Host-header validation rejecting `host.docker.internal` in dev mode, fixed via `%dev.quarkus.http.host-validation.allowed-hosts` in `application.properties`. | QA |
| 5 | Notify the clinical team that step/section renames in the Author Tool will retroactively update step labels in all FHHS reports (SCD Type 1 behavior) — draft ready in `research/Kimball_type2_clinical_notification.md`, not yet sent | Documentation/communication |

**Rollback strategy**: no Flyway down-migration will be authored for `V0.0.8` or any
other Kimball-related FHHS migration. Recovery from a bad rollout is an operational
pre-upgrade database backup/restore, consistent with the Survey and Admin Kimball Type 2
documents.

---

## 9. Summary of Affected Files

| File | Nature of change |
|---|---|
| `src/main/resources/db/migration/V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql` | **New file** — re-applies the triple-negative breast cancer question reordering using Kimball Type 2 versioning protocol (close + insert), idempotent against the existing-deployment case |
| `src/test/resources/db/test/V0.0.0.1__TEST_BOOTSTRAP.sql` | **Updated** — `survey.sections`/`questions`/`sections_questions` now carry the Kimball Type 2 columns (durable id, `version`, `effective_from`/`effective_to`, `published_by`, `published_comment`, `is_draft`; `question_version`/`section_version` on `sections_questions`), plus a trigger per table that defaults the durable id to the fixture's surrogate `id` so `V0.0.1`'s unmodified INSERTs still populate a valid post-Kimball shape |
| `src/test/java/com/elicitsoftware/flyway/CancerQuestionReorderMigrationTest.java` | **New file** — `@QuarkusTest` asserting `V0.0.8`'s end state (triple-negative question at `display_order = 8`, no duplicate "current" rows, the shifted neighbor question moved to `9`) |
| `CancerHistoryRepository.java` | No change — already queries `fact_sections_view` directly with no hardcoded surrogate keys |
| `Respondent.java` | No change |
| `Survey.java` | No change |
| `MigrationService.java` | **Deleted** — superseded by `ManualSchemaMigrator.java` (see section 6) |
| `src/main/java/com/elicitsoftware/flyway/ManualSchemaMigrator.java` | **New file** — dual greenfield/brownfield Flyway track routing, ported from Survey |
| `src/main/resources/db/migration/V0.0.1__POPULATE_FHHS_DATA.sql` | **Fixed** — renamed `metadata`/`relationships` columns to the Kimball names, added durable-id-matches-surrogate-id assignment (`currval()`) to `steps`/`sections`/`questions` inserts |
| `src/main/resources/db/migration-v3/*.sql` | **New directory** — frozen, byte-for-byte copy of the pre-fix `db/migration`, preserving checksums for already-deployed databases |
| `src/test/resources/db/test/V0.0.0.1__TEST_BOOTSTRAP.sql` | **Updated again** — `metadata`/`relationships` renamed to match the fixed `V0.0.1`; `steps` gained a `step_id` durable column + default trigger |
| `src/test/resources/db/test-legacy/V0.0.0.1__TEST_BOOTSTRAP.sql` | **New file** — frozen pre-fix copy of the bootstrap, paired with `db/migration-v3` for upgrade-path testing |
| `src/test/java/com/elicitsoftware/flyway/ManualSchemaMigratorUpgradeTest.java` | **New file** — proves an existing, already-migrated FHHS database converges cleanly onto `db/migration` without any data rewrite |
| All service classes (`casummary`, `proband`, `pedigree`, `familyhistory`) | No change |
