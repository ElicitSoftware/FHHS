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

-- =============================================================================
-- TEST-ONLY bootstrap migration (version 0.0.0.1 -> runs before V0.0.1).
-- =============================================================================
-- FHHS's db/migration scripts are NOT self-contained: they INSERT/UPDATE objects
-- owned by the Survey module (surveys, questions, sections_questions, ...) and
-- read from surveyreport.fact_sections_view, which is populated by Survey's ETL —
-- none of that exists on a throwaway test container. On the shared production
-- database these objects already exist. This script creates the minimal set
-- needed for db/migration to apply cleanly and for the @QuarkusTest suite to
-- exercise real report-generation code paths.
--
-- The survey.* portion mirrors the equivalent bootstrap in the Admin module
-- (src/test/resources/db/test/V0.0.0.1__TEST_BOOTSTRAP.sql there) since both
-- apps assume the same Survey-owned schema. It only includes the tables FHHS's
-- own migrations or entities actually touch — see the per-table comments below.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Roles. The Testcontainers `postgres` user is a superuser, so CREATE ROLE
--    works. PostgreSQL has no CREATE ROLE IF NOT EXISTS, so guard each one.
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'surveyadmin_user') THEN
        CREATE ROLE surveyadmin_user;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'survey_user') THEN
        CREATE ROLE survey_user;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'elicit_owner') THEN
        CREATE ROLE elicit_owner;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'surveyreport_user') THEN
        CREATE ROLE surveyreport_user;
    END IF;
END
$$;

-- -----------------------------------------------------------------------------
-- 2. Schemas. `survey` is also auto-created by quarkus.flyway.owner.schemas, so
--    guard it; `surveyreport` is not managed by Flyway and must be created here.
-- -----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS survey;
CREATE SCHEMA IF NOT EXISTS surveyreport;

-- -----------------------------------------------------------------------------
-- 3. survey.surveys — FK target for reports/respondents/sections_questions/etc.
--    Columns match the Survey entity so Panache can read/write it. FHHS's own
--    V0.0.1 inserts its own row via nextval(), so no seed row is needed here.
-- -----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS survey.surveys_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.surveys
(
    id                  bigint NOT NULL,
    display_order       integer,
    name                character varying(255),
    title               character varying(255),
    description         character varying(2000),
    initial_display_key character varying(255),
    post_survey_url     character varying(2000),
    CONSTRAINT surveys_pk PRIMARY KEY (id)
);

-- -----------------------------------------------------------------------------
-- 4. survey.respondents — read via the Respondent entity (first_access_dt drives
--    report timing) and is the FK target fact_sections_view.respondent_id
--    conceptually points at. Columns match the Respondent entity.
-- -----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS survey.respondents_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.respondents
(
    id              bigint NOT NULL,
    created_dt      timestamptz DEFAULT CURRENT_TIMESTAMP,
    first_access_dt timestamptz,
    finalized_dt    timestamptz,
    active          boolean DEFAULT true,
    logins          integer DEFAULT 0,
    survey_id       bigint NOT NULL,
    token           character varying(255),
    CONSTRAINT respondents_pk PRIMARY KEY (id),
    CONSTRAINT respondents_surveys_fk FOREIGN KEY (survey_id) REFERENCES survey.surveys (id)
);

-- -----------------------------------------------------------------------------
-- 5. survey.reports — V0.0.2__LOAD_REPORTS.sql INSERTs into this without
--    creating it, so it must pre-exist. Also an eager @OneToMany child of
--    Survey (ReportDefinition entity), so loading a Survey queries it.
-- -----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS survey.reports_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.reports
(
    id            bigint NOT NULL,
    survey_id     bigint NOT NULL,
    name          character varying(255),
    description   character varying(2000),
    url           character varying(2000),
    display_order integer,
    CONSTRAINT reports_pk PRIMARY KEY (id),
    CONSTRAINT reports_surveys_fk FOREIGN KEY (survey_id) REFERENCES survey.surveys (id)
);

-- -----------------------------------------------------------------------------
-- 6. Dimension/authoring tables V0.0.1__POPULATE_FHHS_DATA.sql inserts into
--    directly (dimensions, ontology, select_groups, questions, sections,
--    sections_questions, steps, steps_sections, metadata). None of these are
--    created by FHHS's own migrations, so all must pre-exist. Columns copied
--    verbatim from the Survey module's schema (same source Admin's bootstrap
--    uses), since FHHS's INSERTs reference every column below.
-- -----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS survey.dimensions_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.dimensions
(
    id   integer NOT NULL DEFAULT NEXTVAL('survey.dimensions_seq'),
    name character varying(50),
    CONSTRAINT dimensions_pk PRIMARY KEY (id),
    CONSTRAINT dimensions_un UNIQUE (name)
);

CREATE SEQUENCE IF NOT EXISTS survey.ontology_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.ontology
(
    id        integer                NOT NULL,
    survey_id integer                NOT NULL,
    name      character varying(255) NOT NULL,
    tag       character varying(255) NOT NULL,
    dimension integer,
    CONSTRAINT ontology_pk PRIMARY KEY (id),
    CONSTRAINT ontology_dimensions_fk FOREIGN KEY (dimension) REFERENCES survey.dimensions (id),
    CONSTRAINT ontology_un UNIQUE (name, tag)
);

CREATE SEQUENCE IF NOT EXISTS survey.select_groups_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.select_groups
(
    id          integer NOT NULL,
    survey_id   integer NOT NULL,
    name        character varying(255),
    description character varying(255),
    data_type   character varying(50) NOT NULL DEFAULT 'Text',
    CONSTRAINT select_groups_pk PRIMARY KEY (id),
    CONSTRAINT select_groups_name_un UNIQUE (survey_id, name)
);

CREATE SEQUENCE IF NOT EXISTS survey.question_types_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.question_types
(
    id          integer NOT NULL,
    name        character varying(255),
    data_type   character varying(255),
    description character varying(255),
    CONSTRAINT question_types_pk PRIMARY KEY (id),
    CONSTRAINT question_types_name_un UNIQUE (name)
);
INSERT INTO survey.question_types (id, name, data_type, description)
VALUES (1, 'Text', 'Text', 'Seeded by test bootstrap'),
       (4, 'Section', 'Text', 'Seeded by test bootstrap'),
       (5, 'Number', 'Number', 'Seeded by test bootstrap'),
       (7, 'Select', 'Text', 'Seeded by test bootstrap'),
       (8, 'Text Input', 'Text', 'Seeded by test bootstrap'),
       (10, 'MultiSelect', 'Text', 'Seeded by test bootstrap')
ON CONFLICT (id) DO NOTHING;

CREATE SEQUENCE IF NOT EXISTS survey.questions_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.questions
(
    id              integer NOT NULL,
    survey_id       integer NOT NULL,
    type_id         integer NOT NULL,
    text            character varying(8000) NOT NULL,
    short_text      character varying(100),
    tool_tip        character varying(255),
    required        boolean NOT NULL DEFAULT false,
    min_value       integer,
    max_value       integer,
    validation_text character varying(255),
    select_group_id integer,
    mask            character varying(255),
    placeholder     character varying(255),
    default_value   character varying(255),
    variant         character varying(255),
    CONSTRAINT questions_pk PRIMARY KEY (id),
    CONSTRAINT select_groups_fk FOREIGN KEY (select_group_id)
        REFERENCES survey.select_groups (id),
    CONSTRAINT type_fk FOREIGN KEY (type_id)
        REFERENCES survey.question_types (id)
);

CREATE SEQUENCE IF NOT EXISTS survey.sections_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.sections
(
    id             integer NOT NULL,
    survey_id      integer NOT NULL,
    display_order  integer NOT NULL,
    name           character varying(255),
    dimension_name character varying(50) NOT NULL,
    description    character varying(255),
    CONSTRAINT sections_pk PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS survey.sections_questions_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.sections_questions
(
    id            integer NOT NULL,
    survey_id     integer NOT NULL,
    question_id   integer NOT NULL,
    section_id    integer NOT NULL,
    display_order integer NOT NULL,
    CONSTRAINT sections_questions_pk PRIMARY KEY (id),
    CONSTRAINT sections_questions_question_fk FOREIGN KEY (question_id)
        REFERENCES survey.questions (id),
    CONSTRAINT sections_questions_sections_fk FOREIGN KEY (section_id)
        REFERENCES survey.sections (id),
    CONSTRAINT sections_questions_survey_fk FOREIGN KEY (survey_id)
        REFERENCES survey.surveys (id)
);

CREATE SEQUENCE IF NOT EXISTS survey.steps_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.steps
(
    id             integer NOT NULL,
    survey_id      integer NOT NULL,
    display_order  integer NOT NULL,
    name           character varying(255),
    dimension_name character varying(50) NOT NULL,
    description    character varying(255),
    CONSTRAINT steps_pk PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS survey.steps_sections_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.steps_sections
(
    id                    integer               NOT NULL,
    survey_id             integer               NOT NULL,
    step_id               integer               NOT NULL,
    step_display_order    integer               NOT NULL,
    section_id            integer               NOT NULL,
    section_display_order integer               NOT NULL,
    display_key           character varying(34) NOT NULL,
    CONSTRAINT steps_sections_pk PRIMARY KEY (id),
    CONSTRAINT steps_sections_fk FOREIGN KEY (section_id)
        REFERENCES survey.sections (id),
    CONSTRAINT steps_sections_steps_fk FOREIGN KEY (step_id)
        REFERENCES survey.steps (id),
    CONSTRAINT steps_sections_survey_fk FOREIGN KEY (survey_id)
        REFERENCES survey.surveys (id)
);

CREATE SEQUENCE IF NOT EXISTS survey.metadata_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.metadata
(
    id                   integer NOT NULL,
    survey_id            integer NOT NULL,
    step_section_id      integer,
    question_id          integer,
    section_question_id  integer,
    ontology_id          integer NOT NULL,
    value                character varying(255),
    CONSTRAINT metadata_pk PRIMARY KEY (id),
    CONSTRAINT metadata_ontology_fk FOREIGN KEY (ontology_id)
        REFERENCES survey.ontology (id),
    CONSTRAINT metadata_question_fk FOREIGN KEY (question_id)
        REFERENCES survey.questions (id),
    CONSTRAINT metadata_sect_quest_fk FOREIGN KEY (section_question_id)
        REFERENCES survey.sections_questions (id),
    CONSTRAINT metadata_section_fk FOREIGN KEY (step_section_id)
        REFERENCES survey.steps_sections (id),
    CONSTRAINT metadata_survey_fk FOREIGN KEY (survey_id)
        REFERENCES survey.surveys (id)
);

CREATE SEQUENCE IF NOT EXISTS survey.select_items_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.select_items
(
    id            integer NOT NULL,
    survey_id     integer NOT NULL,
    group_id      integer NOT NULL,
    display_text  character varying(255),
    display_order integer NOT NULL,
    coded_value   character varying(255),
    CONSTRAINT select_items_pk PRIMARY KEY (id),
    CONSTRAINT select_items_group_fk FOREIGN KEY (group_id)
        REFERENCES survey.select_groups (id)
);

CREATE SEQUENCE IF NOT EXISTS survey.operator_types_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.operator_types
(
    id          integer NOT NULL,
    name        character varying(255),
    description character varying(255),
    symbol      character varying(10),
    CONSTRAINT operator_types_pk PRIMARY KEY (id),
    CONSTRAINT operator_types_name_un UNIQUE (name)
);
-- Seeded with the exact ids V0.0.1__POPULATE_FHHS_DATA.sql's relationships INSERTs
-- reference (1,2,3,5,6). Names/symbols are placeholders — FHHS never reads this
-- table itself, it's only an FK target so those INSERTs succeed.
INSERT INTO survey.operator_types (id, name, description, symbol)
VALUES (1, 'Equals', 'Seeded by test bootstrap', '='),
       (2, 'NotEquals', 'Seeded by test bootstrap', '!='),
       (3, 'Contains', 'Seeded by test bootstrap', '~'),
       (5, 'IsTrue', 'Seeded by test bootstrap', '=='),
       (6, 'IsFalse', 'Seeded by test bootstrap', '<>')
ON CONFLICT (id) DO NOTHING;

CREATE SEQUENCE IF NOT EXISTS survey.action_types_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.action_types
(
    id          integer NOT NULL,
    name        character varying(255),
    description character varying(255),
    CONSTRAINT action_types_pk PRIMARY KEY (id)
);
-- Seeded with the exact ids V0.0.1's relationships INSERTs reference (1,2,3).
INSERT INTO survey.action_types (id, name, description)
VALUES (1, 'Show', 'Seeded by test bootstrap'),
       (2, 'Hide', 'Seeded by test bootstrap'),
       (3, 'ReplaceText', 'Seeded by test bootstrap')
ON CONFLICT (id) DO NOTHING;

CREATE SEQUENCE IF NOT EXISTS survey.relationships_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.relationships
(
    id                       integer NOT NULL,
    survey_id                integer NOT NULL,
    upstream_step_id         integer,
    upstream_sq_id           integer NOT NULL,
    downstream_step_id       integer,
    downstream_s_id          integer,
    downstream_sq_id         integer,
    operator_id              integer NOT NULL,
    action_id                integer NOT NULL,
    description              character varying(255),
    token                    character varying(10),
    reference_value          character varying(255),
    default_upstream_value   character varying(255),
    override_upstream_value  character varying(255),
    CONSTRAINT relationships_pk PRIMARY KEY (id),
    CONSTRAINT action_fk FOREIGN KEY (action_id)
        REFERENCES survey.action_types (id),
    CONSTRAINT downstream_s_fk FOREIGN KEY (downstream_s_id)
        REFERENCES survey.steps_sections (id),
    CONSTRAINT downstream_sq_fk FOREIGN KEY (downstream_sq_id)
        REFERENCES survey.sections_questions (id),
    CONSTRAINT downstream_step_fk FOREIGN KEY (downstream_step_id)
        REFERENCES survey.steps (id),
    CONSTRAINT operator_fk FOREIGN KEY (operator_id)
        REFERENCES survey.operator_types (id),
    CONSTRAINT relationships_survey_fk FOREIGN KEY (survey_id)
        REFERENCES survey.surveys (id),
    CONSTRAINT upstream_sq_fk FOREIGN KEY (upstream_sq_id)
        REFERENCES survey.sections_questions (id),
    CONSTRAINT upstream_step_fk FOREIGN KEY (upstream_step_id)
        REFERENCES survey.steps (id)
);

-- -----------------------------------------------------------------------------
-- 6b. survey.post_survey_actions — V0.0.2__LOAD_REPORTS.sql INSERTs into this
--    using nextval('survey.post_survey_actions_seq'), but that table/sequence
--    aren't created until V0.0.4__CREATE_POST_SURVEY_ACTIONS.sql, which runs
--    AFTER V0.0.2 in Flyway version order (0.0.2 < 0.0.4). This is a real bug
--    in the shipped migration history — it has always broken a truly fresh
--    install — but V0.0.2/V0.0.4 can't be edited now without breaking Flyway
--    checksum validation on already-migrated production databases. Every real
--    deployment has only ever worked because the shared dev/prod database is
--    never actually fresh when these migrations run. Pre-create it here so
--    V0.0.2 succeeds the same way it always has in practice; V0.0.4's own
--    `CREATE TABLE IF NOT EXISTS` / `CREATE SEQUENCE IF NOT EXISTS` then no-op.
-- -----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS survey.post_survey_actions_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.post_survey_actions
(
    id              integer                 NOT NULL,
    survey_id       integer                 NOT NULL,
    name            character varying(255)  NOT NULL,
    description     character varying(1000),
    url             character varying(500)  NOT NULL,
    execution_order integer                 NOT NULL DEFAULT 1,
    CONSTRAINT post_survey_actions_pk PRIMARY KEY (id),
    CONSTRAINT post_survey_actions_survey_fk FOREIGN KEY (survey_id)
        REFERENCES survey.surveys (id)
);

-- -----------------------------------------------------------------------------
-- 6c. survey.respondent_psa — read/written at runtime by FHHS's post-survey-
--    action retry job (FamilyHistoryReportService's @Scheduled retry loop),
--    not by any FHHS migration. Columns match RespondentPsa entity.
-- -----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS survey.respondent_psa_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS survey.respondent_psa
(
    id                    integer      NOT NULL,
    respondent_id         integer      NOT NULL,
    post_survey_action_id integer      NOT NULL,
    tries                 integer      NOT NULL DEFAULT 0,
    status                varchar(255) NOT NULL,
    error_msg             varchar(255),
    created_dt            timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_dt           timestamptz,
    CONSTRAINT respondent_psa_pk PRIMARY KEY (id),
    CONSTRAINT respondent_psa_psa_fk FOREIGN KEY (post_survey_action_id) REFERENCES survey.post_survey_actions (id),
    CONSTRAINT respondent_psa_respondent_fk FOREIGN KEY (respondent_id) REFERENCES survey.respondents (id)
);

-- -----------------------------------------------------------------------------
-- 7. surveyreport.dim_step / dim_section — Kimball dimension tables. V0.0.6's
--    indexes reference these directly; fact_sections_view's step/section
--    columns come from a join against them in the real ETL.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS surveyreport.dim_step
(
    id    integer NOT NULL,
    value character varying(255),
    CONSTRAINT dim_step_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS surveyreport.dim_section
(
    id    integer NOT NULL,
    value character varying(255),
    CONSTRAINT dim_section_pk PRIMARY KEY (id)
);

-- -----------------------------------------------------------------------------
-- 8. surveyreport.fact_sections — base fact table V0.0.6's indexes are built
--    on. Columns limited to what those indexes reference.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS surveyreport.fact_sections
(
    id               bigint NOT NULL,
    survey_id        integer,
    respondent_id    bigint,
    name             character varying(255),
    step_key         integer,
    section_key      integer,
    step_instance    integer,
    section_instance integer,
    CONSTRAINT fact_sections_pk PRIMARY KEY (id)
);

-- -----------------------------------------------------------------------------
-- 9. surveyreport.fact_respondents — GRANT + index target (V0.0.6).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS surveyreport.fact_respondents
(
    respondent_id bigint,
    survey_id     integer,
    created_key   integer,
    finalized_key integer
);

-- -----------------------------------------------------------------------------
-- 10. surveyreport.fact_sections_view — stands in for the real ETL-populated,
--    dim_step/dim_section-joined view. CancerHistoryRepository reads every
--    demographic/cancer column below directly; V0.0.3__CREATE_FHHS_FACT_VIEW.sql
--    (still applied, then dropped by V0.0.7 later in the same migration run)
--    additionally needs id, step_key, section_key. A plain table works exactly
--    like a view for SELECT purposes, so tests can just INSERT fixture rows.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS surveyreport.fact_sections_view
(
    id                                       bigint NOT NULL,
    respondent_id                            bigint NOT NULL,
    survey_id                                integer,
    name                                     character varying(255),
    step                                     character varying(255),
    step_key                                 integer,
    step_instance                            integer,
    section_key                              integer,
    section_instance                         integer,
    relationship                             character varying(255),
    age                                      integer,
    gender                                   character varying(50),
    generation                               character varying(50),
    race                                     character varying(255),
    vital_status                             character varying(50),
    shared_parent                            character varying(50),
    sibling_type                             character varying(50),
    ashkenazi                                character varying(50),
    latinx                                   character varying(50),
    bladder_cancer                           character varying(50),
    bladder_cancer_age                       integer,
    breast_cancer                            character varying(50),
    breast_cancer_age                        integer,
    triple_negative_breast_cancer            character varying(50),
    colon_or_rectal_cancer                   character varying(50),
    colon_or_rectal_cancer_age               integer,
    endometrial_or_uterine_cancer            character varying(50),
    endometrial_or_uterine_cancer_age        integer,
    kidney_renal_cell_cancer                 character varying(50),
    kidney_renal_cell_cancer_age             integer,
    leukemia                                 character varying(50),
    leukemia_age                             integer,
    lung_cancer                              character varying(50),
    lung_cancer_age                          integer,
    lymphoma                                 character varying(50),
    lymphoma_age                             integer,
    melanoma_skin_cancer                     character varying(50),
    melanoma_skin_cancer_age                 integer,
    nonmelanoma_skin_cancer                  character varying(50),
    nonmelanoma_skin_cancer_age              integer,
    oral_cavity_or_throat_cancer             character varying(50),
    oral_cavity_or_throat_cancer_age         integer,
    other_cancer                             character varying(50),
    other_age                                integer,
    other_cancer_name                        character varying(255),
    ovarian_cancer                           character varying(50),
    ovarian_cancer_age                       integer,
    pancreatic_cancer                        character varying(50),
    pancreatic_cancer_age                    integer,
    prostate_cancer                          character varying(50),
    prostate_cancer_age                      integer,
    stomach_cancer                           character varying(50),
    stomach_cancer_age                       integer,
    testicular_cancer                        character varying(50),
    testicular_cancer_age                    integer,
    thyroid_cancer                           character varying(50),
    thyroid_cancer_age                       integer,
    unknown_cancer                           character varying(50),
    unknown_cancer_age                       integer,
    multiple_bladder_cancers                 character varying(50),
    multiple_breast_cancers                  character varying(50),
    multiple_colon_or_rectal_cancers         character varying(50),
    multiple_endometrial_or_uterine_cancers  character varying(50),
    multiple_kidney_renal_cell_cancers       character varying(50),
    multiple_leukemias                       character varying(50),
    multiple_lung_cancers                    character varying(50),
    multiple_lymphomas                       character varying(50),
    multiple_melanoma_skin_cancers           character varying(50),
    multiple_nonmelanoma_skin_cancers        character varying(50),
    multiple_oral_cavity_or_throat_cancers   character varying(50),
    multiple_other_cancers                   character varying(50),
    multiple_ovarian_cancers                 character varying(50),
    multiple_pancreatic_cancers              character varying(50),
    multiple_prostate_cancers                character varying(50),
    multiple_stomach_cancers                 character varying(50),
    multiple_testicular_cancers              character varying(50),
    multiple_thyroid_cancers                 character varying(50),
    CONSTRAINT fact_sections_view_pk PRIMARY KEY (id)
);

-- -----------------------------------------------------------------------------
-- 11. Let the application roles use the schemas (postgres owns every object
--    here, so the later per-object GRANTs in db/migration succeed regardless).
-- -----------------------------------------------------------------------------
GRANT USAGE ON SCHEMA survey TO survey_user, surveyadmin_user;
GRANT USAGE ON SCHEMA surveyreport TO survey_user, surveyadmin_user, surveyreport_user;
