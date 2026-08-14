package com.elicitsoftware.familyhistory;

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

import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SftpServiceAuthenticationTest {

    @Inject
    SftpService sftpService;

    @Test
    public void testSftpConnection() {
        Log.info("Testing SFTP connection with SSH key authentication...");

        try {
            boolean connectionSuccess = sftpService.testConnection();
            if (connectionSuccess) {
                Log.info("✅ SFTP connection test PASSED - SSH key authentication is working!");
            } else {
                Log.error("❌ SFTP connection test FAILED - Check configuration and logs");
            }
        } catch (Exception e) {
            Log.errorv(e, "❌ SFTP connection test FAILED with exception: {}", e.getMessage());
        }
    }
}
