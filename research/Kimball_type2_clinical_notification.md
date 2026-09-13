# Clinical Team Notification — Step/Section Renames Now Retroactive

> Draft for step 5 of `research/Kimball_type2.md` section 8 (Implementation Steps).
> Send as-is or adapt; not yet sent as of this writing.

---

**Subject: Action needed — renaming survey steps/sections in the Author Tool now changes historical reports**

Hi team,

A recent update to the Survey platform (Kimball Type 2 versioning) changes how step and
section renames behave in the Author Tool, and it affects the Family Health History
reports FHHS generates.

**What changed:** If someone renames a step or section in the Author Tool (for example,
changing "Sibling" to "Brother or Sister"), that new name will now show up on **every**
existing report that references that step or section — not just reports generated after
the rename. This is by design in the new versioning system, but it means a rename is
retroactive: past PDFs regenerated after the change will show the new label, even for
respondents who filled out the survey under the old name.

**Why it matters clinically:** A label change could make an older report read differently
than the context in which it was originally collected, especially if the rename reflects
a meaningful redefinition rather than just wording. Reports should be spot-checked for
coherence after any such rename.

**What we're asking:**
1. Whenever a step or section name is changed in the Author Tool, let us know beforehand
   if possible.
2. After a rename, review a sample of existing FHHS reports (pedigree and cancer summary)
   to confirm the new label still makes sense in context for previously-collected data.
3. No action is needed for the reverse case — new content (new steps/sections/questions)
   doesn't affect old reports.

No code change is required on our end for this — it's expected behavior of the new
system. This note is just to make sure renames get a quick sanity check before/after they
go out.

Happy to walk through an example if useful.
