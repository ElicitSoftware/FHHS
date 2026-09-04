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

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@code SftpService#createAuthenticatedSession()}'s authentication-method
 * selection logic (SSH key takes precedence over password; neither configured is an error).
 * {@code JSch#getSession(...)} does not open a network connection, so this can be exercised
 * without a real SFTP server - unlike {@code SftpServiceLogicTest}/{@code SftpStandaloneTest}
 * in this package, which are manual `main()`-driven scripts requiring a live server.
 */
class SftpServiceAuthSelectionTest {

    private SftpService newService(String host, String username, String password, String privateKey) throws Exception {
        SftpService service = new SftpService();
        setField(service, "sftpHost", host);
        setField(service, "sftpUsername", username);
        setField(service, "sftpPassword", Optional.ofNullable(password));
        setField(service, "sftpPrivateKey", Optional.ofNullable(privateKey));
        setField(service, "sftpPort", 22);
        setField(service, "sftpTimeout", 30000);
        return service;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SftpService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Session invokeCreateAuthenticatedSession(SftpService service) throws Exception {
        Method method = SftpService.class.getDeclaredMethod("createAuthenticatedSession");
        method.setAccessible(true);
        try {
            return (Session) method.invoke(service);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof JSchException) {
                throw (JSchException) e.getCause();
            }
            throw e;
        }
    }

    @Test
    void neitherPasswordNorKeyConfigured_throwsDescriptiveJSchException() throws Exception {
        SftpService service = newService("host", "user", null, null);

        JSchException ex = assertThrows(JSchException.class, () -> invokeCreateAuthenticatedSession(service));
        assertTrue(ex.getMessage().contains("No authentication method configured"));
    }

    @Test
    void passwordOnly_createsSessionBoundToConfiguredHostUserAndPort() throws Exception {
        SftpService service = newService("sftp.example.com", "fhhs_user", "secret", null);

        Session session = invokeCreateAuthenticatedSession(service);

        assertEquals("sftp.example.com", session.getHost());
        assertEquals("fhhs_user", session.getUserName());
        assertEquals(22, session.getPort());
    }

    @Test
    void blankPasswordAndBlankKey_isTreatedAsNotConfigured() throws Exception {
        SftpService service = newService("host", "user", "   ", "  ");

        JSchException ex = assertThrows(JSchException.class, () -> invokeCreateAuthenticatedSession(service));
        assertTrue(ex.getMessage().contains("No authentication method configured"));
    }

    @Test
    void privateKeyFilePath_notFound_throwsDescriptiveJSchException() throws Exception {
        SftpService service = newService("host", "user", null, "/definitely/does/not/exist/id_rsa");

        JSchException ex = assertThrows(JSchException.class, () -> invokeCreateAuthenticatedSession(service));
        assertTrue(ex.getMessage().contains("Private key file not found"));
    }

    @Test
    void keyAndPasswordBothConfigured_keyTakesPrecedence() throws Exception {
        // If password were used instead, getSession() would succeed with no exception at all
        // (setPassword() never validates). Getting the file-not-found error instead proves
        // the key branch was taken, exactly as SftpService's javadoc documents.
        SftpService service = newService("host", "user", "secret", "/definitely/does/not/exist/id_rsa");

        JSchException ex = assertThrows(JSchException.class, () -> invokeCreateAuthenticatedSession(service));
        assertTrue(ex.getMessage().contains("Private key file not found"));
    }

    @Test
    void relativePrivateKeyPath_isResolvedAgainstWorkingDirectoryNotClasspath() throws Exception {
        SftpService service = newService("host", "user", null, "relative/path/to/key");

        JSchException ex = assertThrows(JSchException.class, () -> invokeCreateAuthenticatedSession(service));
        assertTrue(ex.getMessage().contains(System.getProperty("user.dir")),
                "relative key paths must be resolved against user.dir per SftpService javadoc: " + ex.getMessage());
    }
}
