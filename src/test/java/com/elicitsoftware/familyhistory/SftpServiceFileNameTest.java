package com.elicitsoftware.familyhistory;

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

import com.elicitsoftware.test.LogCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-004 BR-007 and BR-008 at the last point before the write: {@link SftpService#uploadFile}
 * accepts only a plain file name in the configured directory, and never logs the name. The
 * service is a plain, unconfigured instance, so a call that gets past the name check fails
 * while connecting - which is how the tests tell the two apart.
 */
class SftpServiceFileNameTest {

    private static final byte[] DATA = "data".getBytes(StandardCharsets.UTF_8);

    @ParameterizedTest
    @ValueSource(strings = {"12345678.pdf", "12345678-index.xml", "A-1_b.2.pdf", "MRN 0042.pdf", ".hidden.pdf"})
    void plainNames_areAccepted(String fileName) {
        assertTrue(SftpService.isPlainFileName(fileName));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", ".", "..", "../x.pdf", "a/b.pdf", "a\\b.pdf", "/abs.pdf", "x\ny.pdf", "x\u0000y.pdf"})
    void namesThatLeaveTheDirectory_areRefused(String fileName) {
        assertFalse(SftpService.isPlainFileName(fileName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../x.pdf", "a/b.pdf", "a\\b.pdf", ".."})
    void uploadFile_refusesTheNameBeforeConnecting(String fileName) {
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> new SftpService().uploadFile(fileName, DATA));

        assertFalse(refusal.getMessage().contains(fileName));
    }

    /** NFR-009: the file name is built from the XID, so a failed upload is logged by size. */
    @Test
    void uploadFile_failure_isLoggedWithoutTheFileName() {
        try (LogCapture log = LogCapture.start()) {
            Exception failure = assertThrows(Exception.class,
                    () -> new SftpService().uploadFile("XID-MARKER-99.pdf", DATA));

            assertFalse(failure instanceof IllegalArgumentException, "the name itself is accepted");
            assertFalse(log.contains("XID-MARKER-99"), log.text());
        }
    }
}
