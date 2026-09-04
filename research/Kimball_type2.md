# Kimball Type 2 Impact on FHHS

> **Status (2026-09-04):** Survey's Kimball Type 2 design (`Survey/research/Kimball_type_2.md`)
> is fully resolved but **not yet implemented** — Survey's migrations stop at `V009` and
> Admin's stop at `V0.0.11` (export format still `V1`). Survey has only test-only scaffolding
> (`com.elicitsoftware.scd`, all `@Disabled`, plus `V011__SCD_Spec_Fixture.sql`) staged for
> when the real migration lands. This document was revised to drop content that referred to
> `surveyreport.FACT_FHHS_VIEW` and `FactFHHSView.java` — both were removed from FHHS in
> `V0.0.7__DROP_FHHS_FACT_VIEW.sql` (commit `548202c`, "Removed FHHS_FACT_VIEW and optimized
> queries", 2026-03-11), which predates the original version of this document. The risk that
> section was written to address no longer exists in the current codebase.

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

### Required fix: new migration using durable keys

Create `V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql` (the next free FHHS migration version —
`V0.0.7` is already used by `V0.0.7__DROP_FHHS_FACT_VIEW.sql`) that performs the same
reordering logic using durable keys and the Type 2 versioning protocol (close old row,
insert new row). This migration must run **after** Survey's Kimball Flyway migrations:

```sql
-- V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql
-- Re-orders the triple-negative breast cancer question using Kimball Type 2
-- versioning protocol. Replaces the direct UPDATE in V0.0.5, which used surrogate ids.
-- This migration must be applied after Survey's Kimball Type 2 Flyway migrations.

-- Step 1: Identify the durable section_id for the cancer section
-- (confirm this value from the live DB after Kimball migration runs)
-- SELECT section_id FROM survey.sections
--  WHERE dimension_name = 'cancer'
--    AND effective_from <= NOW() AND effective_to > NOW();

-- Step 2: For each sections_questions row in that section where display_order > 7,
-- close the current version and insert a new version with display_order + 1.
DO $$
DECLARE
    rec RECORD;
    new_version INT;
    cancer_section_durable_id INT;
BEGIN
    -- Look up durable section_id by stable dimension name
    SELECT section_id INTO cancer_section_durable_id
      FROM survey.sections
     WHERE dimension_name = 'cancer'  -- replace with actual dimension_name
       AND effective_from <= NOW() AND effective_to > NOW()
     LIMIT 1;

    FOR rec IN
        SELECT sq.id, sq.sections_question_id, sq.question_id, sq.section_id,
               sq.survey_id, sq.display_order, sq.version,
               sq.question_version, sq.section_version
          FROM survey.sections_questions sq
         WHERE sq.section_id  = cancer_section_durable_id
           AND sq.display_order > 7
           AND sq.effective_from <= NOW() AND sq.effective_to > NOW()
    LOOP
        -- Close current version
        UPDATE survey.sections_questions
           SET effective_to = NOW(),
               published_by = 'fhhs_migration_v008',
               published_comment = 'Reorder cancer questions post-Kimball Type 2'
         WHERE sections_question_id = rec.sections_question_id
           AND effective_to = '9999-12-31 23:59:59+00';

        -- Insert new version with incremented display_order
        INSERT INTO survey.sections_questions
            (sections_question_id, version, survey_id, section_id, question_id,
             display_order, effective_from, effective_to,
             published_by, published_comment, is_draft,
             question_version, section_version)
        VALUES
            (rec.sections_question_id, rec.version + 1, rec.survey_id,
             rec.section_id, rec.question_id,
             rec.display_order + 1, NOW(), '9999-12-31 23:59:59+00',
             'fhhs_migration_v008',
             'Reorder cancer questions post-Kimball Type 2',
             false,
             rec.question_version, rec.section_version);
    END LOOP;

    -- Move triple-negative breast cancer question to display_order = 8
    -- Identify it by the durable question_id (confirm from survey DB)
    FOR rec IN
        SELECT sq.sections_question_id, sq.question_id, sq.section_id,
               sq.survey_id, sq.version, sq.question_version, sq.section_version
          FROM survey.sections_questions sq
          JOIN survey.questions q ON q.question_id = sq.question_id
                                 AND q.effective_from <= NOW()
                                 AND q.effective_to > NOW()
         WHERE sq.section_id = cancer_section_durable_id
           AND LOWER(q.text) LIKE '%triple%negative%'  -- adjust to match actual text
           AND sq.effective_from <= NOW() AND sq.effective_to > NOW()
    LOOP
        UPDATE survey.sections_questions
           SET effective_to = NOW(),
               published_by = 'fhhs_migration_v008',
               published_comment = 'Reorder triple-negative breast cancer question'
         WHERE sections_question_id = rec.sections_question_id
           AND effective_to = '9999-12-31 23:59:59+00';

        INSERT INTO survey.sections_questions
            (sections_question_id, version, survey_id, section_id, question_id,
             display_order, effective_from, effective_to,
             published_by, published_comment, is_draft,
             question_version, section_version)
        VALUES
            (rec.sections_question_id, rec.version + 1, rec.survey_id,
             rec.section_id, rec.question_id,
             8, NOW(), '9999-12-31 23:59:59+00',
             'fhhs_migration_v008',
             'Reorder triple-negative breast cancer question',
             false,
             rec.question_version, rec.section_version);
    END LOOP;
END $$;
```

> **Important**: The `dimension_name` and question text fragment in the PL/pgSQL block
> above are placeholders. Before writing the final migration, verify the actual
> `dimension_name` of the cancer section and the exact question text for the
> triple-negative question in the live database.

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

## 6. `MigrationService.java` — No Changes Required

`MigrationService` handles Flyway programmatic migrations for FHHS. As long as the new
FHHS migration (`V0.0.8`) is placed in the standard Flyway migration path,
`MigrationService` picks it up automatically.

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
| 1 | Confirm the durable `section_id` value for the cancer section by querying the Kimball-migrated DB | — (discovery) |
| 2 | Write `V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql` using the confirmed durable `section_id`; validate the `dimension_name` and question text fragment against the live DB before committing | `src/main/resources/db/migration/` |
| 3 | Deploy FHHS to staging; verify `CancerHistoryRepository.findFamilyHistoryByRespondentId()` returns correct rows for a known respondent | QA |
| 4 | Verify pedigree and cancer summary reports render correctly for existing respondents | QA |
| 5 | Notify the clinical team that step/section renames in the Author Tool will retroactively update step labels in all FHHS reports (SCD Type 1 behavior) | Documentation/communication |

---

## 9. Summary of Affected Files

| File | Nature of change |
|---|---|
| `src/main/resources/db/migration/V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql` | **New file** — re-applies the triple-negative breast cancer question reordering using Kimball Type 2 versioning protocol (close + insert) |
| `CancerHistoryRepository.java` | No change — already queries `fact_sections_view` directly with no hardcoded surrogate keys |
| `Respondent.java` | No change |
| `Survey.java` | No change |
| `MigrationService.java` | No change |
| All service classes (`casummary`, `proband`, `pedigree`, `familyhistory`) | No change |
