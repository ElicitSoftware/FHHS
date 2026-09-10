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
 * <p>The container runs as the {@code postgres} superuser so the bootstrap migration
 * can {@code CREATE ROLE}. Flyway (on the {@code owner} datasource) then applies the
 * bootstrap and the full {@code db/migration} history at startup.</p>
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("survey")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Override
    public Map<String, String> start() {
        POSTGRES.start();
        String url = POSTGRES.getJdbcUrl();
        String user = POSTGRES.getUsername();
        String password = POSTGRES.getPassword();

        Map<String, String> config = new HashMap<>();
        // Default datasource (all Panache entities / EntityManager use this one).
        config.put("quarkus.datasource.jdbc.url", url);
        config.put("quarkus.datasource.username", user);
        config.put("quarkus.datasource.password", password);
        // Owner datasource (runs Flyway as elicit_owner in prod; same DB in test).
        config.put("quarkus.datasource.owner.jdbc.url", url);
        config.put("quarkus.datasource.owner.username", user);
        config.put("quarkus.datasource.owner.password", password);
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
