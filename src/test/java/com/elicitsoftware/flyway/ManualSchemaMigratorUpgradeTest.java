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

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises {@link ManualSchemaMigrator}'s upgrade branch — the one path no
 * {@code @QuarkusTest} in this suite touches, since {@code PostgresTestResource} always boots
 * a genuinely fresh Testcontainers Postgres, so {@code ManualSchemaMigrator} always takes the
 * greenfield {@code db/migration} branch before any test method runs. {@code db/migration-v3}
 * (the frozen, pre-Kimball-column-name track) has had zero automated coverage.
 * <p>
 * This is a plain JUnit test, not {@code @QuarkusTest} — {@code ManualSchemaMigrator} calls the
 * Flyway Java API directly, so its exact branching logic can be replicated here against a
 * throwaway container without needing CDI at all.
 * <p>
 * Seeds a "v2.x" pre-Kimball, already-fully-migrated FHHS database by running
 * {@code db/migration-v3}'s complete V0.0.1-V0.0.8 (the frozen, old-column-name copy) against a
 * matching old-shape bootstrap ({@code db/test-legacy}, a copy of {@code db/test} taken before
 * this fix — kept separate because the real {@code db/test} bootstrap now reflects the *new*
 * column names, which the old {@code db/migration-v3} migrations don't know about). Then drives
 * {@code ManualSchemaMigrator}'s exact sequence: validate against {@code db/migration} (must
 * fail, since the fixed V0.0.1 has a different checksum than what's recorded), migrate via
 * {@code db/migration-v3} (a full no-op — nothing new to apply), repair against
 * {@code db/migration}, then validate again (must now succeed) — proving an existing,
 * already-migrated FHHS deployment converges cleanly once this fix ships, without re-running or
 * rewriting any of its data.
 */
class ManualSchemaMigratorUpgradeTest {

    private static final String PASSWORD = "SURVEYPW";
    private static final String OWNER_USER = "elicit_owner";

    private PostgreSQLContainer container;

    @BeforeEach
    void startContainer() {
        container = new PostgreSQLContainer("postgres:17")
                .withDatabaseName("survey")
                .withInitScript("db/testcontainers-init.sql");
        container.start();
    }

    @AfterEach
    void stopContainer() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void upgradeTrack_appliedToExistingV2xHistory_convergesOnGreenfieldSchema() throws SQLException {
        // Seed a "v2.x", already-fully-migrated FHHS database: apply the frozen db/migration-v3
        // track (plus its matching old-shape bootstrap) in full, matching how a real existing
        // production FHHS database's Flyway history looks today.
        flywayFor("classpath:db/test-legacy,classpath:db/migration-v3").migrate();

        // ManualSchemaMigrator.validatesCleanly(): db/migration's V0.0.1 now uses the new
        // Kimball column names, so its checksum no longer matches this "v2.x" history's
        // V0.0.1 — this must fail validation, exactly like a real un-upgraded database would.
        assertThrows(FlywayValidateException.class,
                () -> flywayFor("classpath:db/test,classpath:db/migration").validate(),
                "A pre-fix history must NOT validate cleanly against db/migration -- its V0.0.1 "
                        + "checksum differs now that the Kimball column names are baked in");

        // ManualSchemaMigrator's upgrade branch: migrate via db/migration-v3 (a full no-op here
        // -- there is no new version beyond what the seed step already applied), then repair
        // against db/migration so every future boot's validate() succeeds there directly.
        flywayFor("classpath:db/test-legacy,classpath:db/migration-v3").migrate();
        flywayFor("classpath:db/test,classpath:db/migration").repair();

        // This is the whole point of repair(): the NEXT boot must validate cleanly against
        // db/migration with no further routing through db/migration-v3.
        assertDoesNotThrow(() -> flywayFor("classpath:db/test,classpath:db/migration").validate(),
                "After repair(), db/migration must validate cleanly so every future boot uses it directly");

        // repair() only rewrites checksums in the history table -- it never touches actual
        // table data or schema. The pre-existing rows this "v2.x" database seeded must still be
        // present, and its metadata/relationships tables must still be shaped the way
        // db/migration-v3's (old-column-name) migrations left them -- proving this fix doesn't
        // require, or silently trigger, any rewrite of an existing deployment's live data.
        assertPreExistingLegacyDataUntouched();
    }

    private Flyway flywayFor(String locations) {
        Map<String, String> placeholders = Map.of(
                "survey_user", "survey_user",
                "surveyadmin_user", "surveyadmin_user",
                "surveyreport_user", "surveyreport_user");

        return Flyway.configure()
                .dataSource(container.getJdbcUrl(), OWNER_USER, PASSWORD)
                .schemas("survey")
                .table("flyway_fhhs_history")
                .locations(locations.split(","))
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .baselineDescription("Empty Database")
                .validateOnMigrate(true)
                .connectRetries(10)
                // Mirrors ManualSchemaMigrator: a greenfield-only version that the upgrade
                // track has not applied yet (V0.0.9) is pending, not a validation failure.
                .ignoreMigrationPatterns("*:pending")
                .placeholders(placeholders)
                .load();
    }

    private void assertPreExistingLegacyDataUntouched() throws SQLException {
        try (Connection conn = DriverManager.getConnection(container.getJdbcUrl(), OWNER_USER, PASSWORD)) {
            assertEquals(1, countColumn(conn, "metadata", "step_section_id"),
                    "the seeded v2.x database's metadata table must still carry the old column "
                            + "name -- repair() must not rewrite live schema/data");
            assertEquals(1, countColumn(conn, "relationships", "downstream_s_id"),
                    "the seeded v2.x database's relationships table must still carry the old "
                            + "column name -- repair() must not rewrite live schema/data");
            assertEquals(0, countColumn(conn, "steps", "step_id"),
                    "the seeded v2.x database's steps table predates the durable step_id column "
                            + "entirely -- confirms this really is an old-shape database, not one "
                            + "that accidentally picked up the new fixture");
        }
    }

    private long countColumn(Connection conn, String table, String column) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'survey' "
                        + "AND table_name = ? AND column_name = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
