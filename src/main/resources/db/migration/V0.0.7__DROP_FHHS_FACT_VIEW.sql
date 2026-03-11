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

-- FACT_FHHS_VIEW is no longer queried by application code.
-- Drop the view to simplify schema maintenance.
DROP VIEW IF EXISTS surveyreport.FACT_FHHS_VIEW;
