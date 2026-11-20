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

CREATE OR REPLACE VIEW surveyreport.FACT_FHHS_VIEW as
(
-- Family Members
select f1.id,
       f1.respondent_id,
       f1.name,
       f1.age::Numeric as age, f1.gender,
       f1.generation,
       f1.relationship::Numeric as relationship, f1.step,
       f1.step_instance::Numeric as step_instance, f1.race,
       f1.shared_parent,
       f1.sibling_type,
       f1.ashkenazi,
       f1.latinx,
       f1.vital_status,
       f2.bladder_cancer_age::Numeric as bladder_cancer_age, f2.bladder_cancer,
       f2.breast_cancer_age::Numeric as breast_cancer_age, f2.breast_cancer,
       f2.colon_or_rectal_cancer_age::Numeric as colon_or_rectal_cancer_age, f2.colon_or_rectal_cancer,
       f2.endometrial_or_uterine_cancer_age::Numeric as endometrial_or_uterine_cancer_age, f2.endometrial_or_uterine_cancer,
       f2.kidney_renal_cell_cancer_age::Numeric as kidney_renal_cell_cancer_age, f2.kidney_renal_cell_cancer,
       f2.leukemia_age::Numeric as leukemia_age, f2.leukemia,
       f2.lung_cancer_age::Numeric as lung_cancer_age, f2.lung_cancer,
       f2.lymphoma_age::Numeric as lymphoma_age, f2.lymphoma,
       f2.melanoma_skin_cancer_age::Numeric as melanoma_skin_cancer_age, f2.melanoma_skin_cancer,
       f2.multiple_bladder_cancers,
       f2.multiple_breast_cancers,
       f2.multiple_colon_or_rectal_cancers,
       f2.multiple_endometrial_or_uterine_cancers,
       f2.multiple_kidney_renal_cell_cancers,
       f2.multiple_leukemias,
       f2.multiple_lung_cancers,
       f2.multiple_lymphomas,
       f2.multiple_melanoma_skin_cancers,
       f2.multiple_nonmelanoma_skin_cancers,
       f2.multiple_oral_cavity_or_throat_cancers,
       f2.multiple_other_cancers,
       f2.multiple_ovarian_cancers,
       f2.multiple_pancreatic_cancers,
       f2.multiple_prostate_cancers,
       f2.multiple_stomach_cancers,
       f2.multiple_testicular_cancers,
       f2.multiple_thyroid_cancers,
       f2.nonmelanoma_skin_cancer_age::Numeric as nonmelanoma_skin_cancer_age, f2.nonmelanoma_skin_cancer,
       f2.oral_cavity_or_throat_cancer_age::Numeric as oral_cavity_or_throat_cancer_age, f2.oral_cavity_or_throat_cancer,
       f2.other_age::Numeric as other_age, f2.other_cancer,
       f2.other_cancer_name,
       f2.ovarian_cancer_age::Numeric as ovarian_cancer_age, f2.ovarian_cancer,
       f2.pancreatic_cancer_age::Numeric as pancreatic_cancer_age, f2.pancreatic_cancer,
       f2.prostate_cancer_age::Numeric as prostate_cancer_age, f2.prostate_cancer,
       f2.stomach_cancer_age::Numeric as stomach_cancer_age, f2.stomach_cancer,
       f2.testicular_cancer_age::Numeric as testicular_cancer_age, f2.testicular_cancer,
       f2.thyroid_cancer_age::Numeric as thyroid_cancer_age, f2.thyroid_cancer,
       f2.triple_negative_breast_cancer,
       f2.unknown_cancer_age::Numeric as unknown_cancer_age, f2.unknown_cancer
FROM surveyreport.fact_sections_view f1
         JOIN surveyreport.fact_sections_view f2 on f1.respondent_id = f2.respondent_id
    and f1.step_key = f2.step_key
    and f1.step_instance = f2.step_instance
    and f1.section_instance = f2.section_instance
    and f1.section_key = 1
	and f2.section_key = 2
where f1.step_key != 3
  	and f2.step_key != 4    
UNION
--Family Members without cancer
select f3.id,
       f3.respondent_id,
       f3.name,
       f3.age::Numeric as age, f3.gender,
       f3.generation,
       f3.relationship::Numeric as relationship, f3.step,
       f3.step_instance::Numeric as step_instance, f3.race,
       f3.shared_parent,
       f3.sibling_type,
       f3.ashkenazi,
       f3.latinx,
       f3.vital_status,
       f3.bladder_cancer_age::Numeric as bladder_cancer_age, f3.bladder_cancer,
       f3.breast_cancer_age::Numeric as breast_cancer_age, f3.breast_cancer,
       f3.colon_or_rectal_cancer_age::Numeric as colon_or_rectal_cancer_age, f3.colon_or_rectal_cancer,
       f3.endometrial_or_uterine_cancer_age::Numeric as endometrial_or_uterine_cancer_age, f3.endometrial_or_uterine_cancer,
       f3.kidney_renal_cell_cancer_age::Numeric as kidney_renal_cell_cancer_age, f3.kidney_renal_cell_cancer,
       f3.leukemia_age::Numeric as leukemia_age, f3.leukemia,
       f3.lung_cancer_age::Numeric as lung_cancer_age, f3.lung_cancer,
       f3.lymphoma_age::Numeric as lymphoma_age, f3.lymphoma,
       f3.melanoma_skin_cancer_age::Numeric as melanoma_skin_cancer_age, f3.melanoma_skin_cancer,
       f3.multiple_bladder_cancers,
       f3.multiple_breast_cancers,
       f3.multiple_colon_or_rectal_cancers,
       f3.multiple_endometrial_or_uterine_cancers,
       f3.multiple_kidney_renal_cell_cancers,
       f3.multiple_leukemias,
       f3.multiple_lung_cancers,
       f3.multiple_lymphomas,
       f3.multiple_melanoma_skin_cancers,
       f3.multiple_nonmelanoma_skin_cancers,
       f3.multiple_oral_cavity_or_throat_cancers,
       f3.multiple_other_cancers,
       f3.multiple_ovarian_cancers,
       f3.multiple_pancreatic_cancers,
       f3.multiple_prostate_cancers,
       f3.multiple_stomach_cancers,
       f3.multiple_testicular_cancers,
       f3.multiple_thyroid_cancers,
       f3.nonmelanoma_skin_cancer_age::Numeric as nonmelanoma_skin_cancer_age, f3.nonmelanoma_skin_cancer,
       f3.oral_cavity_or_throat_cancer_age::Numeric as oral_cavity_or_throat_cancer_age, f3.oral_cavity_or_throat_cancer,
       f3.other_age::Numeric as other_age, f3.other_cancer,
       f3.other_cancer_name,
       f3.ovarian_cancer_age::Numeric as ovarian_cancer_age, f3.ovarian_cancer,
       f3.pancreatic_cancer_age::Numeric as pancreatic_cancer_age, f3.pancreatic_cancer,
       f3.prostate_cancer_age::Numeric as prostate_cancer_age, f3.prostate_cancer,
       f3.stomach_cancer_age::Numeric as stomach_cancer_age, f3.stomach_cancer,
       f3.testicular_cancer_age::Numeric as testicular_cancer_age, f3.testicular_cancer,
       f3.thyroid_cancer_age::Numeric as thyroid_cancer_age, f3.thyroid_cancer,
       f3.triple_negative_breast_cancer,
       f3.unknown_cancer_age::Numeric as unknown_cancer_age, f3.unknown_cancer
	FROM surveyreport.fact_sections_view f3
where f3.step_key != 3
	and f3.section_key = 1
  	and f3.step_key != 4
	and NOT EXISTS (
    	SELECT 1
    	from surveyreport.fact_sections_view f4
    	WHERE f4.section_key = 2
		and f3.step = f4.step
		and f3.step_instance = f4.step_instance
        and f3.respondent_id = f4.respondent_id
	)
UNION
-- Proband 
select distinct f1.id,
       f1.respondent_id,
       f1.name,
       f1.age::Numeric as age, f1.gender,
       f1.generation,
       f1.relationship::Numeric as relationship, f1.step,
       f1.step_instance::Numeric as step_instance, f1.race,
       f1.shared_parent,
       f1.sibling_type,
       f1.ashkenazi,
       f1.latinx,
       f1.vital_status,
       f2.bladder_cancer_age::Numeric as bladder_cancer_age, f2.bladder_cancer,
       f2.breast_cancer_age::Numeric as breast_cancer_age, f2.breast_cancer,
       f2.colon_or_rectal_cancer_age::Numeric as colon_or_rectal_cancer_age, f2.colon_or_rectal_cancer,
       f2.endometrial_or_uterine_cancer_age::Numeric as endometrial_or_uterine_cancer_age, f2.endometrial_or_uterine_cancer,
       f2.kidney_renal_cell_cancer_age::Numeric as kidney_renal_cell_cancer_age, f2.kidney_renal_cell_cancer,
       f2.leukemia_age::Numeric as leukemia_age, f2.leukemia,
       f2.lung_cancer_age::Numeric as lung_cancer_age, f2.lung_cancer,
       f2.lymphoma_age::Numeric aslymphoma_age, f2.lymphoma,
       f2.melanoma_skin_cancer_age::Numeric as melanoma_skin_cancer_age, f2.melanoma_skin_cancer,
       f2.multiple_bladder_cancers,
       f2.multiple_breast_cancers,
       f2.multiple_colon_or_rectal_cancers,
       f2.multiple_endometrial_or_uterine_cancers,
       f2.multiple_kidney_renal_cell_cancers,
       f2.multiple_leukemias,
       f2.multiple_lung_cancers,
       f2.multiple_lymphomas,
       f2.multiple_melanoma_skin_cancers,
       f2.multiple_nonmelanoma_skin_cancers,
       f2.multiple_oral_cavity_or_throat_cancers,
       f2.multiple_other_cancers,
       f2.multiple_ovarian_cancers,
       f2.multiple_pancreatic_cancers,
       f2.multiple_prostate_cancers,
       f2.multiple_stomach_cancers,
       f2.multiple_testicular_cancers,
       f2.multiple_thyroid_cancers,
       f2.nonmelanoma_skin_cancer_age::Numeric as nonmelanoma_skin_cancer_age, f2.nonmelanoma_skin_cancer,
       f2.oral_cavity_or_throat_cancer_age::Numeric as oral_cavity_or_throat_cancer_age, f2.oral_cavity_or_throat_cancer,
       f2.other_age::Numeric as other_age, f2.other_cancer,
       f2.other_cancer_name,
       f2.ovarian_cancer_age::Numeric as ovarian_cancer_age, f2.ovarian_cancer,
       f2.pancreatic_cancer_age::Numeric as pancreatic_cancer_age, f2.pancreatic_cancer,
       f2.prostate_cancer_age::Numeric as prostate_cancer_age, f2.prostate_cancer,
       f2.stomach_cancer_age::Numeric as stomach_cancer_age, f2.stomach_cancer,
       f2.testicular_cancer_age::Numeric as testicular_cancer_age, f2.testicular_cancer,
       f2.thyroid_cancer_age::Numeric as thyroid_cancer_age, f2.thyroid_cancer,
       f2.triple_negative_breast_cancer,
       f2.unknown_cancer_age::Numeric as unknown_cancer_age, f2.unknown_cancer
FROM surveyreport.fact_sections_view f1
LEFT JOIN surveyreport.fact_sections_view f2 on f1.respondent_id = f2.respondent_id and f2.step_key = 4
where f1.step_key = 3
);

GRANT SELECT ON surveyreport.FACT_FHHS_VIEW TO ${surveyreport_user};
GRANT SELECT ON surveyreport.FACT_FHHS_VIEW TO ${survey_user};
