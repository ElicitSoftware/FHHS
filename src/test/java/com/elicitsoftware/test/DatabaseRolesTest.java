package com.elicitsoftware.test;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The suite's database connections are the ones FHHS has on a site, and neither is a superuser
 * (umbrella C-020). No FHHS use case describes its own test database, so this cites the
 * platform constraint: every report use case (UC-001 to UC-004) reads through the application
 * connection, and a grant that connection lacks must fail a test as it would fail a site. That
 * only holds while the connections stay what {@link PostgresTestResource} makes them, which is
 * what these tests pin down.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class DatabaseRolesTest {

    @Inject
    EntityManager entityManager;

    @Inject
    @DataSource("owner")
    AgroalDataSource ownerDataSource;

    @Test
    @Transactional
    void applicationConnectsAsSurveyUserWithoutSuperuser() {
        assertEquals("survey_user", entityManager.createNativeQuery("SELECT current_user").getSingleResult());
        assertFalse((Boolean) entityManager
                        .createNativeQuery("SELECT rolsuper FROM pg_roles WHERE rolname = current_user").getSingleResult(),
                "the application role is not a superuser");
    }

    /** The application role reads what it was granted and cannot create objects in Survey's schema. */
    @Test
    @Transactional
    void applicationRoleHoldsItsGrantsAndNoMore() {
        assertTrue((Boolean) entityManager
                        .createNativeQuery("SELECT has_table_privilege(current_user, 'survey.respondents', 'SELECT')")
                        .getSingleResult(),
                "granted by Survey's migrations");
        assertFalse((Boolean) entityManager
                        .createNativeQuery("SELECT has_schema_privilege(current_user, 'survey', 'CREATE')").getSingleResult(),
                "only the schema owner creates objects in the survey schema");
        assertFalse((Boolean) entityManager
                        .createNativeQuery("SELECT has_database_privilege(current_user, current_database(), 'CREATE')")
                        .getSingleResult(),
                "only the schema owner creates a reporting schema");
    }

    @Test
    void migrationsRunAsTheSchemaOwnerWithoutSuperuser() throws SQLException {
        try (Connection connection = ownerDataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery(
                     "SELECT current_user, rolsuper, rolcreaterole, rolcreatedb, "
                             + "has_database_privilege(current_user, current_database(), 'CREATE') "
                             + "FROM pg_roles WHERE rolname = current_user")) {
            assertTrue(row.next());
            assertEquals("elicit_owner", row.getString(1));
            assertFalse(row.getBoolean(2), "the schema owner is not a superuser");
            assertFalse(row.getBoolean(3), "the schema owner cannot create roles");
            assertFalse(row.getBoolean(4), "the schema owner cannot create databases");
            assertTrue(row.getBoolean(5), "the schema owner creates a survey's reporting schema");
        }
    }
}
