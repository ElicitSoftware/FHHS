package com.elicitsoftware.test;

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

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Starts a single PostgreSQL container (Testcontainers) and points BOTH Quarkus
 * datasources — the default and the {@code owner} datasource — at it.
 *
 * <p>FHHS's own Flyway migrations assume the base {@code survey}/{@code surveyreport}
 * schema (owned by the Survey app) already exists. There is no real Survey/Postgres
 * available in CI, so this resource pairs with the test-only bootstrap migration
 * ({@code db/test/V0.0.0.1__TEST_BOOTSTRAP.sql}) that creates the minimal set of
 * cross-module objects FHHS's migrations need, on a throwaway container.</p>
 *
 * <p>Neither datasource connects as a superuser, because neither does on a site. The
 * container's own {@code postgres} superuser runs one thing, {@code db/testcontainers-init.sql},
 * which prepares the cluster as the installation manual has an operator do it: the roles and
 * the two empty schemas. The {@code owner} datasource then connects as {@code elicit_owner},
 * and Flyway applies the bootstrap and the full {@code db/migration} history as that role; the
 * default datasource connects as {@code survey_user}, with the grants Survey's and FHHS's
 * migrations give it and no others. A migration that needs more than the schema owner has, or
 * a table the application role was never granted, therefore fails here as it would on a site.</p>
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("survey")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withInitScript("db/testcontainers-init.sql");

    // The roles db/testcontainers-init.sql creates, with the password it gives both.
    private static final String APPLICATION_USER = "survey_user";
    private static final String OWNER_USER = "elicit_owner";
    private static final String PASSWORD = "SURVEYPW";

    @Override
    public Map<String, String> start() {
        POSTGRES.start();
        String url = POSTGRES.getJdbcUrl();

        Map<String, String> config = new HashMap<>();
        // Default datasource (all Panache entities / EntityManager use this one).
        config.put("quarkus.datasource.jdbc.url", url);
        config.put("quarkus.datasource.username", APPLICATION_USER);
        config.put("quarkus.datasource.password", PASSWORD);
        // Owner datasource (runs Flyway as elicit_owner, here as on a site; same DB).
        config.put("quarkus.datasource.owner.jdbc.url", url);
        config.put("quarkus.datasource.owner.username", OWNER_USER);
        config.put("quarkus.datasource.owner.password", PASSWORD);
        // Turn off Dev Services now that we supply explicit URLs.
        config.put("quarkus.datasource.devservices.enabled", "false");
        config.put("quarkus.datasource.owner.devservices.enabled", "false");
        return config;
    }

    @Override
    public void stop() {
        POSTGRES.stop();
    }
}
