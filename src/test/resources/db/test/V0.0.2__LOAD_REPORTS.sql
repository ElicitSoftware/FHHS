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
-- TEST FIXTURE (UC-005). The report definitions and the post-survey action the Family History
-- Survey needs, seeded here for the tests only; a deployment gets them from the imported
-- FHHS/family-history-survey.elicit, which carries both.
-- Literal ids and fixed keys: see the identity rules at the top of V0.0.1.
-- REPORTS --
INSERT INTO survey.reports(id, survey_id, name, description, url, display_order, report_key) VALUES (1, 1, 'Respondent', 'Respondent', 'http://host.docker.internal:8082/proband/report', 0, '3b53e682-e521-509a-8d18-3a22e50401c4');
INSERT INTO survey.reports(id, survey_id, name, description, url, display_order, report_key) VALUES (2, 1, 'Cancer Summary', 'Cancer Summary', 'http://host.docker.internal:8082/casummary/report', 1, 'fde15f3a-57ea-5066-ac75-b55181a2a92e');
INSERT INTO survey.reports(id, survey_id, name, description, url, display_order, report_key) VALUES (3, 1, 'Family Pedigree', 'Family Pedigree', 'http://host.docker.internal:8082/pedigree/report', 2, '854f50d7-da7d-51e4-ad52-926851b2be9e');
-- POST SURVEY ACTIONS --
INSERT INTO survey.post_survey_actions(id, survey_id, name, description, url, execution_order, post_survey_action_key)
VALUES (1, 1,
        'Generate Family History Report',
        'Automatically generates a family history PDF report and uploads it to SFTP server after survey completion',
        'http://host.docker.internal:8082/familyhistory/generate',
        1, '9f57b644-fecd-557d-a466-07ec19bb95c4');
-- SEQUENCES --
SELECT setval('survey.reports_seq', 3);
SELECT setval('survey.post_survey_actions_seq', 1);
