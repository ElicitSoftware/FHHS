package com.elicitsoftware.flyway;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import io.quarkus.agroal.DataSource;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Runs Flyway manually against a database-detected brownfield/greenfield/converged location,
// replacing Quarkus's migrate-at-start (deliberately disabled — see the comment on
// quarkus.flyway.owner.migrate-at-start in application.properties for why a
// FlywayConfigurationCustomizer cannot do this: Quarkus resolves classpath: migration locations
// at BUILD time, so mutating FluentConfiguration.locations() at runtime has no effect on which
// files actually get executed). This is the same mechanism the sibling Survey app uses
// (com.elicitsoftware.flyway.ManualSchemaMigrator there) — ported here because FHHS's own
// foundational V0.0.1__POPULATE_FHHS_DATA.sql referenced pre-Kimball column names
// (survey.metadata's step_section_id/section_question_id, survey.relationships'
// downstream_s_id) that don't exist on Survey's greenfield Kimball schema. See
// FHHS/research/Kimball_type2.md and the plan at
// /Users/mdemerat/.claude/plans/i-have-reset-the-whimsical-lampson.md.
//
// Tries db/migration (the greenfield, Kimball-column-aware schema) first. If it validates
// cleanly there — either a genuinely fresh database, or a v2.x database that has already been
// through the upgrade-and-repair cycle below on a prior boot — it stays there permanently; every
// future migration only ever needs to exist in this one location. (src/test/resources/db/test's
// test-only data fixtures live in a separate Flyway location layered on top of this one only
// under the %test profile.) If db/migration's checksums DON'T match (a v2.x database that hasn't
// been upgraded yet), it migrates via db/migration-v3 (the frozen, pre-Kimball-column-name
// history) instead, then immediately repairs its history against db/migration so the NEXT boot's
// validate() against db/migration succeeds and this database converges onto it for good.
// The two locations are siblings, not nested — Flyway's classpath location scanning is recursive,
// so db/migration-v3 must NOT live under db/migration.
@ApplicationScoped
public class ManualSchemaMigrator {

    private static final String GREENFIELD_LOCATION_SUFFIX = "db/migration";
    private static final String UPGRADE_LOCATION_SUFFIX = "db/migration-v3";

    @Inject
    @DataSource("owner")
    javax.sql.DataSource ownerDataSource;

    @ConfigProperty(name = "quarkus.flyway.owner.schemas")
    String schemas;

    @ConfigProperty(name = "quarkus.flyway.owner.table")
    String table;

    @ConfigProperty(name = "quarkus.flyway.owner.locations")
    String configuredLocations;

    @ConfigProperty(name = "quarkus.flyway.owner.baseline-on-migrate")
    boolean baselineOnMigrate;

    @ConfigProperty(name = "quarkus.flyway.owner.baseline-version")
    String baselineVersion;

    @ConfigProperty(name = "quarkus.flyway.owner.baseline-description")
    String baselineDescription;

    @ConfigProperty(name = "quarkus.flyway.owner.validate-on-migrate")
    boolean validateOnMigrate;

    @ConfigProperty(name = "quarkus.flyway.owner.connect-retries")
    int connectRetries;

    @ConfigProperty(name = "quarkus.flyway.owner.placeholders.survey_user")
    String surveyUser;

    @ConfigProperty(name = "quarkus.flyway.owner.placeholders.surveyadmin_user")
    String surveyAdminUser;

    @ConfigProperty(name = "quarkus.flyway.owner.placeholders.surveyreport_user")
    String surveyReportUser;

    // Explicit @Priority beats any unprioritized StartupEvent observer regardless of the
    // numeric value chosen; a low value is used anyway to be unambiguous about intent.
    void migrate(@Observes @Priority(1) StartupEvent event) {
        List<String> greenfieldLocations = Arrays.stream(configuredLocations.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (validatesCleanly(greenfieldLocations)) {
            // Either a genuinely fresh database, or a v2.x database that already converged on a
            // prior boot (see the repair() call below) — db/migration alone is correct either way.
            buildFlyway(greenfieldLocations).migrate();
            return;
        }

        Log.infof("db/migration checksum mismatch (unupgraded v2.x history) — routing Flyway to %s",
                UPGRADE_LOCATION_SUFFIX);
        List<String> upgradeLocations = greenfieldLocations.stream().map(this::toUpgradeLocation).toList();
        buildFlyway(upgradeLocations).migrate();

        Log.infof("Upgrade complete — realigning history against %s so future boots use it directly",
                GREENFIELD_LOCATION_SUFFIX);
        buildFlyway(greenfieldLocations).repair();
    }

    // True if this database's currently recorded history has no checksum conflicts with
    // db/migration — i.e. it's safe to treat db/migration as authoritative going forward.
    private boolean validatesCleanly(List<String> greenfieldLocations) {
        if (!historyTableExists()) {
            // Genuinely fresh FHHS install — nothing recorded yet, so nothing to validate.
            // Unlike Survey (which owns the survey schema itself, so "schema doesn't exist yet"
            // reliably means "fresh"), FHHS shares that schema with Survey/Admin: by the time
            // FHHS's ManualSchemaMigrator ever runs, the survey schema already exists (Survey
            // creates it first — FHHS's docker-compose depends_on: survey: condition:
            // service_healthy). So checking schema existence is never a valid "is this fresh"
            // signal here; checking FHHS's own history TABLE is. Flyway's validate() does not
            // tolerate a missing history table either (throws FlywayValidateException, exactly
            // like the missing-schema case Survey's version guards against) — checking first
            // avoids misreading "fresh FHHS install" as "needs the v2.x upgrade path".
            return true;
        }
        try {
            buildFlyway(greenfieldLocations).validate();
            return true;
        } catch (FlywayValidateException e) {
            return false;
        }
    }

    private boolean historyTableExists() {
        try (Connection connection = ownerDataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ?")) {
            ps.setString(1, schemas);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private Flyway buildFlyway(List<String> locations) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("survey_user", surveyUser);
        placeholders.put("surveyadmin_user", surveyAdminUser);
        placeholders.put("surveyreport_user", surveyReportUser);

        return Flyway.configure()
                .dataSource(ownerDataSource)
                .schemas(schemas)
                .table(table)
                .locations(locations.toArray(new String[0]))
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .baselineDescription(baselineDescription)
                .validateOnMigrate(validateOnMigrate)
                .connectRetries(connectRetries)
                // Flyway's validate() treats a legitimately PENDING (not-yet-applied) migration
                // as a validation failure by default, indistinguishable from a real checksum
                // mismatch without this. That matters here because validatesCleanly() uses
                // validate() as its "is this database on the greenfield track" probe: after any
                // migration fails partway through a boot (e.g. V0.0.3 depending on Survey's ETL
                // having run at least once — see DeploymentScript.md's restart-then-restart
                // fresh-install sequence) and the app restarts, the remaining pending versions
                // would otherwise be misread as "unupgraded v2.x history", routing a genuinely
                // fresh/greenfield database through db/migration-v3 (the legacy-column-name
                // track) and failing for real. Ignoring pending-migration errors here restricts
                // validate()'s verdict to actual checksum mismatches, which is the only signal
                // that legitimately means "needs the upgrade track".
                .ignoreMigrationPatterns("*:pending")
                .placeholders(placeholders)
                .load();
    }

    private String toUpgradeLocation(String location) {
        if (!location.endsWith(GREENFIELD_LOCATION_SUFFIX)) {
            return location;
        }
        return location.substring(0, location.length() - GREENFIELD_LOCATION_SUFFIX.length()) + UPGRADE_LOCATION_SUFFIX;
    }
}
