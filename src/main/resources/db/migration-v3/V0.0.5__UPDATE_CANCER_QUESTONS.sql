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

-- The tripple negative breast cancer question was added at the end of the section.
-- Reorder the questions to move it up. 
-- Move the questions down, making room for the triple negative question
UPDATE survey.sections_questions
set display_order = display_order + 1
where section_id = 14
  and display_order > 7;

-- Insert the triple negative question under breast cancer
UPDATE survey.sections_questions
set display_order = 8
where section_id = 14
  and id = 125;
