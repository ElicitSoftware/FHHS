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
@Startup
public class MigrationService {

    @Inject
    @FlywayDataSource("owner")
    Flyway flywayForOwner;

    @ConfigProperty(name = "quarkus.flyway.owner.migrate-at-start")
    boolean migrateAtStart;

    @PostConstruct
    public void migrationServiceCheck() {
        if (!migrateAtStart) {
            checkMigration();
        }
    }

    public void checkMigration() {
        // Use the flyway instance manually
        flywayForOwner.repair();
        flywayForOwner.migrate();
        System.out.println(flywayForOwner.info().current().getVersion().toString());
    }
}
