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

INSERT INTO survey.reports(id, survey_id, name, description, url, display_order) VALUES(nextval('survey.relationships_seq'), 1, 'Respondent', 'Respondent', 'http://host.docker.internal:8082/proband/report', 0);
INSERT INTO survey.reports(id, survey_id, name, description, url, display_order) VALUES(nextval('survey.relationships_seq'),1, 'Cancer Summary', 'Cancer Summary', 'http://host.docker.internal:8082/casummary/report', 1);
INSERT INTO survey.reports(id, survey_id, name, description, url, display_order) VALUES(nextval('survey.relationships_seq'),1, 'Family Pedigree', 'Family Pedigree', 'http://host.docker.internal:8082/pedigree/report', 3);
-- Test Respondent --
-- INSERT INTO survey.respondents (id, survey_id, token, active, logins, created_dt, first_access_dt, finalized_dt) VALUES (NEXTVAL('survey.respondents_seq'), 1, 'test',false, 1, current_date, current_date, current_date);
