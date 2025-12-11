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

import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

/**
 * Service responsible for managing database migrations using Flyway.
 * <p>
 * This service is initialized at application startup and provides functionality
 * to check and execute database migrations. It integrates with Quarkus's Flyway
 * extension to manage the database schema evolution.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
@Startup
public class MigrationService {

    /**
     * Default constructor.
     */
    public MigrationService() {
        // Default constructor for CDI
    }

    /**
     * Flyway instance configured for the "owner" datasource.
     * Injected using Quarkus CDI with the FlywayDataSource qualifier.
     */
    @Inject
    @FlywayDataSource("owner")
    Flyway flywayForOwner;

    /**
     * Configuration property that determines whether migrations should run at startup.
     * Mapped from the "quarkus.flyway.owner.migrate-at-start" configuration property.
     */
    @ConfigProperty(name = "quarkus.flyway.owner.migrate-at-start")
    boolean migrateAtStart;

    /**
     * Post-construction method that checks if manual migration is needed.
     * <p>
     * This method is called after dependency injection is complete. If automatic
     * migration at startup is disabled, it triggers a manual migration check.
     * </p>
     */
    @PostConstruct
    public void migrationServiceCheck() {
        if (!migrateAtStart) {
            checkMigration();
        }
    }

    /**
     * Performs database migration operations including repair and migrate.
     * <p>
     * This method:
     * <ul>
     *   <li>Repairs the Flyway schema history table if necessary</li>
     *   <li>Executes pending migrations</li>
     *   <li>Prints the current database version to the console</li>
     * </ul>
     */
    public void checkMigration() {
        // Use the flyway instance manually
        flywayForOwner.repair();
        flywayForOwner.migrate();
        System.out.println(flywayForOwner.info().current().getVersion().toString());
    }
}
