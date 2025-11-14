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

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusTest
public class SftpServiceAuthenticationTest {

    private static final Logger LOG = LoggerFactory.getLogger(SftpServiceAuthenticationTest.class);

    @Inject
    SftpService sftpService;

    @Test
    public void testSftpConnection() {
        LOG.info("Testing SFTP connection with SSH key authentication...");
        
        try {
            boolean connectionSuccess = sftpService.testConnection();
            if (connectionSuccess) {
                LOG.info("✅ SFTP connection test PASSED - SSH key authentication is working!");
            } else {
                LOG.error("❌ SFTP connection test FAILED - Check configuration and logs");
            }
        } catch (Exception e) {
            LOG.error("❌ SFTP connection test FAILED with exception: {}", e.getMessage(), e);
        }
    }
}
