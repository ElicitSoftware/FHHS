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

import com.jcraft.jsch.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service for handling SFTP file transfers using JSch library.
 * 
 * <p>This service provides functionality to upload files to a remote SFTP server
 * running in Docker on the configured host and port. It uses JSch (Java Secure Channel)
 * library for secure file transfer operations.</p>
 * 
 * <p><strong>Configuration Properties:</strong></p>
 * <ul>
 *   <li><strong>family.history.sftp.host:</strong> SFTP server hostname</li>
 *   <li><strong>family.history.sftp.username:</strong> SFTP username for authentication</li>
 *   <li><strong>family.history.sftp.password:</strong> SFTP password for authentication (optional if using private key)</li>
 *   <li><strong>family.history.sftp.privateKey:</strong> Either a path to private key file OR the actual private key content (PEM format starting with -----BEGIN) for SSH key authentication (optional if using password)</li>
 *   <li><strong>family.history.sftp.path:</strong> Remote directory path for file uploads</li>
 *   <li><strong>family.history.sftp.port:</strong> SFTP server port (default: 22)</li>
 *   <li><strong>family.history.sftp.timeout:</strong> Connection timeout in milliseconds (default: 30000)</li>
 * </ul>
 * 
 * <p><strong>Authentication:</strong> The service supports both password and SSH key authentication.
 * If both password and privateKey are configured, SSH key authentication takes precedence.
 * The privateKey property accepts either a file path or the direct private key content (PEM format).</p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 1.0
 */
@ApplicationScoped
public class SftpService {

    /**
     * Default constructor.
     */
    public SftpService() {
        // Default constructor for CDI
    }

    /**
     * SFTP server hostname. Only required when family.history.sftp.enabled=true.
     */
    @ConfigProperty(name = "family.history.sftp.host")
    Optional<String> sftpHostConfig;

    /**
     * SFTP username for authentication. Only required when family.history.sftp.enabled=true.
     */
    @ConfigProperty(name = "family.history.sftp.username")
    Optional<String> sftpUsernameConfig;
    
    /**
     * Optional password for SFTP authentication.
     */
    @ConfigProperty(name = "family.history.sftp.password")
    Optional<String> sftpPassword;
    
    /**
     * Optional private key for SFTP authentication.
     */
    @ConfigProperty(name = "family.history.sftp.privateKey")
    Optional<String> sftpPrivateKey;
    
    /**
     * The SFTP server's host key: one or more known_hosts lines (as printed by
     * {@code ssh-keyscan -p <port> <host>}) or the path of a known_hosts file. The connection is
     * refused without it, unless {@link #sftpStrictHostKeyChecking} is false.
     */
    @ConfigProperty(name = "family.history.sftp.knownHosts")
    Optional<String> sftpKnownHosts;

    /**
     * false accepts any host key, so the server's identity is not verified. Only for the bundled
     * test SFTP server, which makes a new host key every time it starts.
     */
    @ConfigProperty(name = "family.history.sftp.strictHostKeyChecking", defaultValue = "true")
    boolean sftpStrictHostKeyChecking;

    /**
     * Remote path on SFTP server where files are uploaded. Only required when family.history.sftp.enabled=true.
     */
    @ConfigProperty(name = "family.history.sftp.path")
    Optional<String> sftpPathConfig;
    
    /**
     * SFTP server port (default 22).
     */
    @ConfigProperty(name = "family.history.sftp.port", defaultValue = "22")
    int sftpPort;
    
    /**
     * Connection timeout in milliseconds (default 30000).
     */
    @ConfigProperty(name = "family.history.sftp.timeout", defaultValue = "30000")
    int sftpTimeout;

    /**
     * SFTP server hostname, resolved from {@link #sftpHostConfig}.
     * Only populated (and only needed) when family.history.sftp.enabled=true.
     */
    String sftpHost;

    /**
     * SFTP username, resolved from {@link #sftpUsernameConfig}.
     * Only populated (and only needed) when family.history.sftp.enabled=true.
     */
    String sftpUsername;

    /**
     * Remote upload path, resolved from {@link #sftpPathConfig}.
     * Only populated (and only needed) when family.history.sftp.enabled=true.
     */
    String sftpPath;

    @PostConstruct
    void init() {
        sftpHost = sftpHostConfig.orElse(null);
        sftpUsername = sftpUsernameConfig.orElse(null);
        sftpPath = sftpPathConfig.orElse(null);
    }

    /**
     * Uploads a file to the configured SFTP server using JSch library.
     * 
     * <p>Establishes a secure SFTP connection to the configured server and uploads
     * the provided file data to the specified remote directory. The connection is
     * automatically closed after the upload completes or if an error occurs.</p>
     * 
     * @param fileName the name of the file to upload
     * @param fileData the content of the file as byte array
     * @throws RuntimeException if upload fails due to connection issues or I/O errors
     */
    public void uploadFile(String fileName, byte[] fileData) {
        Session session = null;
        ChannelSftp sftpChannel = null;
        
        try {
            Log.debugv("Connecting to SFTP server {0}:{1} for file upload: {2}", sftpHost, sftpPort, fileName);
            
            // Create JSch session with authentication
            session = createAuthenticatedSession();
            
            // Connect to the session
            session.connect();
            Log.debugv("SSH session connected to {0}:{1}", sftpHost, sftpPort);
            
            // Open SFTP channel
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            Log.debug("SFTP channel opened successfully");
            
            // Ensure the remote directory exists
            ensureRemoteDirectoryExists(sftpChannel, sftpPath);
            
            // Change to the target directory
            sftpChannel.cd(sftpPath);
            
            // Upload the file using OVERWRITE mode to ensure binary transfer
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData)) {
                sftpChannel.put(inputStream, fileName, ChannelSftp.OVERWRITE);
            }
            
            Log.infov("Successfully uploaded file: {0} ({1} bytes) to SFTP server {2}:{3}{4}",
                     fileName, fileData.length, sftpHost, sftpPort, sftpPath);
            
        } catch (JSchException e) {
            Log.errorv(e, "SFTP connection failed for file {0}: {1}", fileName, e.getMessage());
            throw new RuntimeException("Failed to connect to SFTP server", e);
        } catch (SftpException e) {
            Log.errorv(e, "SFTP operation failed for file {0}: {1}", fileName, e.getMessage());
            throw new RuntimeException("Failed to upload file to SFTP server", e);
        } catch (IOException e) {
            Log.errorv(e, "I/O error during file upload {0}: {1}", fileName, e.getMessage());
            throw new RuntimeException("I/O error during file upload", e);
        } finally {
            // Clean up resources
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
                Log.debug("SFTP channel disconnected");
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
                Log.debug("SSH session disconnected");
            }
        }
    }
    
    /**
     * Ensures that the remote directory exists on the SFTP server.
     * Creates the directory structure recursively if it doesn't exist.
     * 
     * @param sftpChannel the active SFTP channel
     * @param remotePath the remote directory path to ensure exists
     * @throws SftpException if directory creation fails
     */
    private void ensureRemoteDirectoryExists(ChannelSftp sftpChannel, String remotePath) throws SftpException {
        try {
            // Try to change to the directory - if it exists, this will succeed
            sftpChannel.cd(remotePath);
            Log.debugv("Remote directory exists: {0}", remotePath);
        } catch (SftpException e) {
            // Directory doesn't exist, create it
            Log.debugv("Creating remote directory: {0}", remotePath);
            
            // Split the path and create directories recursively
            String[] pathParts = remotePath.split("/");
            StringBuilder currentPath = new StringBuilder();
            
            for (String part : pathParts) {
                if (part.isEmpty()) continue; // Skip empty parts from leading/trailing slashes
                
                currentPath.append("/").append(part);
                String dirToCreate = currentPath.toString();
                
                try {
                    sftpChannel.cd(dirToCreate);
                } catch (SftpException ex) {
                    // Directory doesn't exist, create it
                    sftpChannel.mkdir(dirToCreate);
                    Log.debugv("Created remote directory: {0}", dirToCreate);
                    sftpChannel.cd(dirToCreate);
                }
            }
        }
    }
    
    /**
     * Tests the SFTP connection with the configured settings.
     * 
     * <p>Establishes a connection to the SFTP server, verifies access to the
     * configured directory, and checks write permissions without creating files.</p>
     * 
     * @return true if connection is successful and directory is writable, false otherwise
     */
    public boolean testConnection() {
        Session session = null;
        ChannelSftp sftpChannel = null;
        
        try {
            Log.debugv("Testing SFTP connection to {0}:{1}", sftpHost, sftpPort);
            
            // Create JSch session with authentication
            session = createAuthenticatedSession();
            
            // Connect to the session
            session.connect();
            Log.debug("SSH session connected for connection test");
            
            // Open SFTP channel
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            Log.debug("SFTP channel opened for connection test");
            
            // Ensure the remote directory exists
            ensureRemoteDirectoryExists(sftpChannel, sftpPath);
            
            // Change to the target directory and verify access
            sftpChannel.cd(sftpPath);
            
            // Check directory permissions to verify write access
            SftpATTRS attrs = sftpChannel.stat(sftpPath);
            int permissions = attrs.getPermissions();
            boolean writable = (permissions & 0200) != 0; // Check owner write permission
            
            if (!writable) {
                Log.warnv("SFTP directory {0} is not writable (permissions: {1})", sftpPath,
                        Integer.toOctalString(permissions));
                return false;
            }

            Log.infov("SFTP connection test successful to {0}:{1}{2} (writable)", sftpHost, sftpPort, sftpPath);
            return true;

        } catch (JSchException e) {
            Log.errorv(e, "SFTP connection test failed - connection error: {0}", e.getMessage());
            return false;
        } catch (SftpException e) {
            Log.errorv(e, "SFTP connection test failed - SFTP operation error: {0}", e.getMessage());
            return false;
        } finally {
            // Clean up resources
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
                Log.debug("SFTP channel disconnected after connection test");
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
                Log.debug("SSH session disconnected after connection test");
            }
        }
    }
    
    /**
     * Creates an authenticated JSch session using either password or SSH key authentication.
     * SSH key authentication takes precedence if both are configured.
     * 
     * @return configured and ready-to-connect JSch session
     * @throws JSchException if session creation or key loading fails
     */
    private Session createAuthenticatedSession() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(sftpUsername, sftpHost, sftpPort);
        
        // Configure authentication method
        if (sftpPrivateKey.isPresent() && !sftpPrivateKey.get().trim().isEmpty()) {
            // Use SSH key authentication
            String privateKeyValue = sftpPrivateKey.get().trim();
            Log.debugv("Using SSH key authentication with key: {0}",
                     privateKeyValue.startsWith("-----BEGIN") ? "private key content" : privateKeyValue);
            
            try {
                // Check if the value is a private key content (starts with -----BEGIN) or a file path
                if (privateKeyValue.startsWith("-----BEGIN")) {
                    // Direct private key content - add it directly to JSch
                    byte[] privateKeyBytes = privateKeyValue.getBytes();
                    jsch.addIdentity("private-key", privateKeyBytes, null, null);
                    Log.debug("Successfully loaded SSH private key from direct content");
                } else {
                    // File path - load from file
                    File keyFile = new File(privateKeyValue);
                    if (!keyFile.isAbsolute()) {
                        // Make path relative to the application working directory
                        keyFile = new File(System.getProperty("user.dir"), privateKeyValue);
                    }
                    
                    if (!keyFile.exists()) {
                        throw new JSchException("Private key file not found: " + keyFile.getAbsolutePath());
                    }
                    
                    jsch.addIdentity(keyFile.getAbsolutePath());
                    Log.debugv("Successfully loaded SSH private key from: {0}", keyFile.getAbsolutePath());
                }

            } catch (JSchException e) {
                Log.errorv("Failed to load SSH private key: {0}", e.getMessage());
                throw e;
            }

        } else if (sftpPassword.isPresent() && !sftpPassword.get().trim().isEmpty()) {
            // Use password authentication
            Log.debug("Using password authentication");
            session.setPassword(sftpPassword.get());
            
        } else {
            throw new JSchException("No authentication method configured. Please set either family.history.sftp.password or family.history.sftp.privateKey");
        }
        
        configureHostKeyChecking(jsch, session);
        session.setTimeout(sftpTimeout);
        
        return session;
    }

    /** A known_hosts line: hosts, key type, key; hashed hosts (|1|...) included. */
    private static final Pattern KNOWN_HOSTS_LINE =
            Pattern.compile("(?m)^\\s*(@\\S+\\s+)?\\S+\\s+(ssh-|ecdsa-|sk-)\\S+\\s+\\S+");

    /**
     * Verifies the server's host key against {@link #sftpKnownHosts}, so a report carrying PHI is
     * never handed to a server that merely claims the configured address. Without known hosts the
     * session is refused, unless host key checking was turned off explicitly.
     */
    void configureHostKeyChecking(JSch jsch, Session session) throws JSchException {
        String knownHosts = sftpKnownHosts == null ? "" : sftpKnownHosts.map(String::trim).orElse("");
        if (!knownHosts.isEmpty()) {
            if (KNOWN_HOSTS_LINE.matcher(knownHosts).find()) {
                jsch.setKnownHosts(new ByteArrayInputStream(knownHosts.getBytes(StandardCharsets.UTF_8)));
            } else {
                File file = new File(knownHosts);
                if (!file.isAbsolute()) {
                    file = new File(System.getProperty("user.dir"), knownHosts);
                }
                if (!file.isFile()) {
                    throw new JSchException("Known hosts file not found: " + file.getAbsolutePath());
                }
                jsch.setKnownHosts(file.getAbsolutePath());
            }
            session.setConfig("StrictHostKeyChecking", "yes");
        } else if (sftpStrictHostKeyChecking) {
            throw new JSchException("No SFTP host key configured. Set family.history.sftp.knownHosts to the "
                    + "server's known_hosts line(s) (ssh-keyscan -p " + sftpPort + " " + sftpHost
                    + ") or to a known_hosts file");
        } else {
            Log.warnv("SFTP host key checking is off: the identity of {0} is not verified "
                    + "(family.history.sftp.strictHostKeyChecking=false)", sftpHost);
            session.setConfig("StrictHostKeyChecking", "no");
        }
    }
}
