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

-- Runs once, immediately after the Testcontainers Postgres container reports healthy and
-- before Flyway ever connects. Used only by ManualSchemaMigratorUpgradeTest, which drives
-- Flyway directly (bypassing PostgresTestResource/@QuarkusTest) and therefore needs the same
-- roles/schema ownership the real shared database already has. Mirrors the equivalent script
-- in the sibling Survey module.
CREATE ROLE survey_user LOGIN PASSWORD 'SURVEYPW';
CREATE ROLE surveyadmin_user LOGIN PASSWORD 'SURVEYPW';
CREATE ROLE surveyreport_user LOGIN PASSWORD 'SURVEYPW';
CREATE ROLE elicit_owner LOGIN PASSWORD 'SURVEYPW' CREATEROLE CREATEDB;

ALTER DATABASE survey OWNER TO elicit_owner;

CREATE SCHEMA IF NOT EXISTS survey AUTHORIZATION elicit_owner;
CREATE SCHEMA IF NOT EXISTS surveyreport AUTHORIZATION elicit_owner;

GRANT USAGE ON SCHEMA survey, surveyreport TO survey_user, surveyadmin_user, surveyreport_user, elicit_owner;
