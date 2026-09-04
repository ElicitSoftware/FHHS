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
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link MigrationService}. Note the project's own comment on
 * {@code quarkus.flyway.owner.migrate-at-start} in application.properties: "if
 * migrate-at-start is false the MigrationService.Repair() will run!" - these tests lock
 * down that {@code migrationServiceCheck()} only calls {@code checkMigration()} in that
 * exact case.
 */
class MigrationServiceTest {

    private MigrationService newService(Flyway flyway, boolean migrateAtStart) {
        MigrationService service = new MigrationService();
        service.flywayForOwner = flyway;
        service.migrateAtStart = migrateAtStart;
        return service;
    }

    private Flyway flywayReturningVersion(String version) {
        Flyway flyway = mock(Flyway.class);
        MigrationInfoService infoService = mock(MigrationInfoService.class);
        MigrationInfo migrationInfo = mock(MigrationInfo.class);
        when(migrationInfo.getVersion()).thenReturn(MigrationVersion.fromVersion(version));
        when(infoService.current()).thenReturn(migrationInfo);
        when(flyway.info()).thenReturn(infoService);
        return flyway;
    }

    @Test
    void migrationServiceCheck_migrateAtStartFalse_runsCheckMigration() {
        Flyway flyway = flywayReturningVersion("0.0.7");
        MigrationService service = newService(flyway, false);

        service.migrationServiceCheck();

        verify(flyway).repair();
        verify(flyway).migrate();
    }

    @Test
    void migrationServiceCheck_migrateAtStartTrue_doesNotRunCheckMigration() {
        Flyway flyway = mock(Flyway.class);
        MigrationService service = newService(flyway, true);

        service.migrationServiceCheck();

        verify(flyway, never()).repair();
        verify(flyway, never()).migrate();
    }

    @Test
    void checkMigration_alwaysRepairsBeforeMigrating() {
        Flyway flyway = flywayReturningVersion("0.0.7");
        MigrationService service = newService(flyway, false);

        service.checkMigration();

        InOrder order = Mockito.inOrder(flyway);
        order.verify(flyway).repair();
        order.verify(flyway).migrate();
    }
}
