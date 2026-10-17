<!--
  One pull request per issue, from a branch named <type>/<issue>-<slug> off V3.
  Title: what the change does, in the imperative, without the issue number.
  Tick every line that applies; strike one through (~~like this~~) with a reason when it does not.
  An unticked, unstruck line is a question the reviewer will ask.
-->

## Issue

Closes #

## What changed and why

<!-- Two to five sentences. Behavior first, implementation second. -->

## AIUP

<!-- docs/ is the source of truth; the docs commit comes before the code commit. -->

- [ ] `docs/requirements.md` updated (IDs: ) or no requirement changed
- [ ] `docs/use_cases.puml` and the use case spec(s) updated (IDs: ) or no behavior changed
- [ ] `docs/entity_model.md` updated or no entity changed
- [ ] Tests cite the UC and BR IDs they cover

## Checks

- [ ] Flyway migration added to **both** `db/migration` and `db/migration-v3`
- [ ] Brownfield verified with `resetDatabase.sh V2` from the umbrella (every FHHS change, not only migrations)
- [ ] A change to `family-history-survey.elicit` verified greenfield (a V2 database cannot take an update of the definition)
- [ ] The SFTP XML template still uses `{AccessCode}`; the respondent credential is never called a token
- [ ] Cross-module change: the tracking issue in `ElicitSoftware/Elicit` and the sibling PRs are linked above
- [ ] No AI co-author trailer in any commit message
