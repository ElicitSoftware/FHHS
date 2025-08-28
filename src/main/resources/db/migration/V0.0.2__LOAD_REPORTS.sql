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
-- Test Reports --
INSERT INTO survey.reports(id, survey_id, name, description, url, display_order) VALUES(nextval('survey.relationships_seq'), 1, 'Respondent', 'Respondent', 'http://host.docker.internal:8082/proband/report', 0);
INSERT INTO survey.reports(id, survey_id, name, description, url, display_order) VALUES(nextval('survey.relationships_seq'),1, 'Cancer Summary', 'Cancer Summary', 'http://host.docker.internal:8082/casummary/report', 1);
INSERT INTO survey.reports(id, survey_id, name, description, url, display_order) VALUES(nextval('survey.relationships_seq'),1, 'Family Pedigree', 'Family Pedigree', 'http://host.docker.internal:8082/pedigree/report', 3);
-- Post Survey Actions --
INSERT INTO survey.post_survey_actions (id, survey_id, name, description, url, execution_order)
VALUES (
           NEXTVAL('survey.post_survey_actions_seq'),
           1,
           'Generate Family History Report',
           'Automatically generates a family history PDF report and uploads it to SFTP server after survey completion',
           'http://host.docker.internal:8082/familyhistory/generate',
           1
       );
