package com.elicitsoftware.familyhistory;

/*-
 * ***LICENSE_START***
 * Elicit Survey
 * %%
 * Copyright (C) 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-004 BR-006, NFR-007: the family history report, which carries PHI, is handed only to an
 * SFTP server whose host key the site configured. Exercised against a real SSH server
 * (Apache MINA SSHD) so the handshake itself proves which keys are accepted.
 */
class SftpServiceHostKeyTest {

    private SshServer server;
    private KeyPair hostKey;

    @BeforeEach
    void startServer() throws Exception {
        hostKey = hostKeyPair();
        server = SshServer.setUpDefaultServer();
        server.setHost("localhost");
        server.setPort(0);
        server.setKeyPairProvider(KeyPairProvider.wrap(hostKey));
        server.setPasswordAuthenticator((user, password, session) -> "secret".equals(password));
        server.start();
    }

    @AfterEach
    void stopServer() throws Exception {
        server.stop(true);
    }

    /** ECDSA P-256: a host key type both JSch and MINA SSHD handle with the JDK alone. */
    private static KeyPair hostKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        return generator.generateKeyPair();
    }

    private String knownHostsLine(KeyPair key) {
        return "[localhost]:" + server.getPort() + " " + PublicKeyEntry.toString(key.getPublic());
    }

    private SftpService service(String knownHosts, boolean strict) throws Exception {
        SftpService service = new SftpService();
        set(service, "sftpHost", "localhost");
        set(service, "sftpUsername", "fhhs_user");
        set(service, "sftpPassword", Optional.of("secret"));
        set(service, "sftpPrivateKey", Optional.empty());
        set(service, "sftpKnownHosts", Optional.ofNullable(knownHosts));
        set(service, "sftpStrictHostKeyChecking", strict);
        set(service, "sftpPort", server.getPort());
        set(service, "sftpTimeout", 10_000);
        return service;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = SftpService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void connect(SftpService service) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession("fhhs_user", "localhost", service.sftpPort);
        session.setPassword("secret");
        service.configureHostKeyChecking(jsch, session);
        session.setTimeout(10_000);
        try {
            session.connect();
        } finally {
            session.disconnect();
        }
    }

    @Test
    void configuredHostKey_connects() throws Exception {
        assertDoesNotThrow(() -> connect(service(knownHostsLine(hostKey), true)));
    }

    @Test
    void differentHostKey_isRefused() throws Exception {
        // A server presenting a key other than the configured one: an impostor at the address.
        JSchException ex = assertThrows(JSchException.class,
                () -> connect(service(knownHostsLine(hostKeyPair()), true)));
        assertTrue(ex.getMessage().toLowerCase().contains("hostkey") || ex.getMessage().contains("host key"),
                ex.getMessage());
    }

    @Test
    void noKnownHosts_isRefusedBeforeConnecting() throws Exception {
        SftpService service = service(null, true);
        JSchException ex = assertThrows(JSchException.class, () -> connect(service));
        assertTrue(ex.getMessage().startsWith("No SFTP host key configured"), ex.getMessage());
        assertTrue(ex.getMessage().contains("ssh-keyscan -p " + server.getPort() + " localhost"), ex.getMessage());
    }

    @Test
    void knownHostsFile_connects(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("known_hosts");
        Files.writeString(file, knownHostsLine(hostKey) + "\n");
        assertDoesNotThrow(() -> connect(service(file.toString(), true)));
    }

    @Test
    void missingKnownHostsFile_isRefused() throws Exception {
        JSchException ex = assertThrows(JSchException.class,
                () -> connect(service("/definitely/not/here/known_hosts", true)));
        assertTrue(ex.getMessage().startsWith("Known hosts file not found"), ex.getMessage());
    }

    @Test
    void checkingTurnedOff_acceptsAnyKey() throws Exception {
        assertDoesNotThrow(() -> connect(service(null, false)));
    }

    @Test
    void strictHostKeyChecking_defaultsToOn() throws Exception {
        var annotation = SftpService.class.getDeclaredField("sftpStrictHostKeyChecking")
                .getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class);
        assertEquals("true", annotation.defaultValue());
    }
}
