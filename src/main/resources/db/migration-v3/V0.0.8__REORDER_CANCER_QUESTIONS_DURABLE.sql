---
-- ***LICENSE_START***
-- Elicit FHHS
-- %%
-- Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
-- %%
-- PolyForm Noncommercial License 1.0.0
-- <https://polyformproject.org/licenses/noncommercial/1.0.0>
-- ***LICENSE_END***
---

-- Re-applies the triple-negative breast cancer question reordering originally done by
-- V0.0.5__UPDATE_CANCER_QUESTONS.sql, which used hardcoded surrogate ids (section_id = 14,
-- id = 125). Those surrogate values are unreliable once Survey's Kimball Type 2 migration
-- retargets sections_questions.section_id/question_id to durable ids -- on a fresh install
-- where FHHS's migrations run after Survey's Kimball migration, V0.0.5's WHERE clause can
-- match zero rows, silently leaving the question unordered. This migration finds the same
-- row via durable natural keys (section dimension_name, question short_text) and reorders
-- it using the Type 2 close+insert versioning protocol instead of a direct UPDATE.
--
-- Idempotent: if the triple-negative question's current display_order is already 8 (the
-- normal upgrade path -- V0.0.5 ran correctly pre-Kimball and the Kimball migration carried
-- that value forward unchanged), this migration is a no-op for that section. It only shifts
-- display_order when V0.0.5's effect never landed.
DO $$
DECLARE
    sec RECORD;
    tn  RECORD;
    rec RECORD;
BEGIN
    FOR sec IN
        SELECT section_id, survey_id
          FROM survey.sections
         WHERE dimension_name = 'Cancers'
           AND effective_from <= NOW() AND effective_to > NOW()
    LOOP
        SELECT sq.sections_question_id, sq.display_order, sq.version, sq.question_id,
               sq.question_version, sq.section_version
          INTO tn
          FROM survey.sections_questions sq
          JOIN survey.questions q
            ON q.question_id = sq.question_id
           AND q.effective_from <= NOW() AND q.effective_to > NOW()
         WHERE sq.section_id = sec.section_id
           AND sq.survey_id  = sec.survey_id
           AND sq.effective_from <= NOW() AND sq.effective_to > NOW()
           AND q.short_text = 'Triple Negative'
         LIMIT 1;

        IF tn.sections_question_id IS NULL THEN
            CONTINUE; -- no triple-negative question in this survey's Cancers section
        END IF;

        IF tn.display_order = 8 THEN
            CONTINUE; -- already correctly ordered
        END IF;

        -- Move every other current question in the section down to make room at position 8
        FOR rec IN
            SELECT sq.sections_question_id, sq.display_order, sq.version, sq.survey_id,
                   sq.section_id, sq.question_id, sq.question_version, sq.section_version
              FROM survey.sections_questions sq
             WHERE sq.section_id = sec.section_id
               AND sq.survey_id  = sec.survey_id
               AND sq.sections_question_id != tn.sections_question_id
               AND sq.display_order > 7
               AND sq.effective_from <= NOW() AND sq.effective_to > NOW()
        LOOP
            UPDATE survey.sections_questions
               SET effective_to = NOW(),
                   published_by = 'fhhs_migration_v0.0.8',
                   published_comment = 'Reorder cancer questions post-Kimball Type 2'
             WHERE sections_question_id = rec.sections_question_id
               AND effective_to = '9999-12-31 23:59:59+00';

            INSERT INTO survey.sections_questions
                (id, sections_question_id, version, survey_id, section_id, question_id,
                 display_order, effective_from, effective_to,
                 published_by, published_comment, is_draft,
                 question_version, section_version)
            VALUES
                (nextval('survey.sections_questions_seq'), rec.sections_question_id,
                 rec.version + 1, rec.survey_id, rec.section_id, rec.question_id,
                 rec.display_order + 1, NOW(), '9999-12-31 23:59:59+00',
                 'fhhs_migration_v0.0.8', 'Reorder cancer questions post-Kimball Type 2',
                 false, rec.question_version, rec.section_version);
        END LOOP;

        -- Move the triple-negative question itself to display_order = 8
        UPDATE survey.sections_questions
           SET effective_to = NOW(),
               published_by = 'fhhs_migration_v0.0.8',
               published_comment = 'Reorder triple-negative breast cancer question'
         WHERE sections_question_id = tn.sections_question_id
           AND effective_to = '9999-12-31 23:59:59+00';

        INSERT INTO survey.sections_questions
            (id, sections_question_id, version, survey_id, section_id, question_id,
             display_order, effective_from, effective_to,
             published_by, published_comment, is_draft,
             question_version, section_version)
        VALUES
            (nextval('survey.sections_questions_seq'), tn.sections_question_id,
             tn.version + 1, sec.survey_id, sec.section_id, tn.question_id,
             8, NOW(), '9999-12-31 23:59:59+00',
             'fhhs_migration_v0.0.8', 'Reorder triple-negative breast cancer question',
             false, tn.question_version, tn.section_version);
    END LOOP;
END $$;
