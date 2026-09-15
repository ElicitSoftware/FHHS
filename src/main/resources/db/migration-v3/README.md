# TEMPORARY — this directory is upgrade-path scaffolding, not permanent history

This is a **frozen, byte-for-byte copy** of `db/migration` as it existed before the Kimball
Type 2 SCD (V3.0.0) fix — kept only so `com.elicitsoftware.flyway.ManualSchemaMigrator` can
upgrade an existing, already-deployed pre-Kimball ("V2.x") FHHS database, whose
`flyway_fhhs_history` already has these exact files' checksums recorded.

**Never edit these files.** Any change invalidates the checksum match against real deployed
databases, defeating the entire point of this directory.

**Delete this directory once every real FHHS deployment has upgraded to V3** (i.e. once every
environment's `flyway_fhhs_history` has converged onto `db/migration` — `ManualSchemaMigrator`
logs this on the boot it happens). At that point, also delete:

- `src/main/java/com/elicitsoftware/flyway/ManualSchemaMigrator.java`
- `src/test/resources/db/test-legacy/` (its matching test fixture)
- `src/test/java/com/elicitsoftware/flyway/ManualSchemaMigratorUpgradeTest.java`

and revert `quarkus.flyway.owner.migrate-at-start` in `application.properties` back to
Quarkus-managed auto-migration.

Tracked in `FHHS/research/Kimball_type2.md` (section 6) and the repo-root
`DeploymentScript.md` — update both when this directory is actually removed.
