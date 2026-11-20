---
-- ***LICENSE_START***
-- Elicit FHHS
-- %%
-- Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
-- %%
-- PolyForm Noncommercial License 1.0.0
-- <https://polyformproject.org/licenses/noncommercial/1.0.0>
-- ***LICENSE_END***
---

-- V0.0.1 seeds the Type 2 structural tables with explicit durable ids taken from each
-- table's surrogate sequence (step_id = currval('survey.steps_seq') and so on), which never
-- advances the *_durable_seq sequences that back the durable-id column defaults. The first
-- survey definition applied through Admin afterwards therefore took durable id 1 for its
-- first step/section/question/select group and hit steps_id_version_un (and friends).
-- Move every durable sequence past the highest durable id already in use. Idempotent: it
-- only ever moves a sequence forward.
SELECT setval('survey.steps_durable_seq',
              GREATEST((SELECT last_value FROM survey.steps_durable_seq),
                       (SELECT COALESCE(MAX(step_id), 1) FROM survey.steps)));
SELECT setval('survey.sections_durable_seq',
              GREATEST((SELECT last_value FROM survey.sections_durable_seq),
                       (SELECT COALESCE(MAX(section_id), 1) FROM survey.sections)));
SELECT setval('survey.questions_durable_seq',
              GREATEST((SELECT last_value FROM survey.questions_durable_seq),
                       (SELECT COALESCE(MAX(question_id), 1) FROM survey.questions)));
SELECT setval('survey.select_groups_durable_seq',
              GREATEST((SELECT last_value FROM survey.select_groups_durable_seq),
                       (SELECT COALESCE(MAX(select_group_id), 1) FROM survey.select_groups)));
SELECT setval('survey.select_items_durable_seq',
              GREATEST((SELECT last_value FROM survey.select_items_durable_seq),
                       (SELECT COALESCE(MAX(select_item_id), 1) FROM survey.select_items)));
SELECT setval('survey.steps_sections_durable_seq',
              GREATEST((SELECT last_value FROM survey.steps_sections_durable_seq),
                       (SELECT COALESCE(MAX(steps_sections_id), 1) FROM survey.steps_sections)));
SELECT setval('survey.sections_questions_durable_seq',
              GREATEST((SELECT last_value FROM survey.sections_questions_durable_seq),
                       (SELECT COALESCE(MAX(sections_question_id), 1) FROM survey.sections_questions)));
SELECT setval('survey.relationships_durable_seq',
              GREATEST((SELECT last_value FROM survey.relationships_durable_seq),
                       (SELECT COALESCE(MAX(relationship_id), 1) FROM survey.relationships)));
