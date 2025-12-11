package com.elicitsoftware.common.health;

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

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Health check implementation for database connectivity.
 * <p>
 * This health check verifies that the application can connect to the database
 * and execute queries. It is marked as a readiness check, meaning the application
 * will not receive traffic until this check passes.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    /**
     * The data source used to obtain database connections.
     * Injected by the CDI container.
     */
    @Inject
    DataSource dataSource;

    /**
     * Default constructor.
     * CDI container will instantiate this class and inject dependencies.
     */
    public DatabaseHealthCheck() {
        // Default constructor for CDI
    }

    @Override
    public HealthCheckResponse call() {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            // Simple connectivity test
            statement.execute("SELECT 1");
            
            return HealthCheckResponse.up("Database connection healthy");
        } catch (Exception e) {
            return HealthCheckResponse.down("Database connection failed: " + e.getMessage());
        }
    }
}
