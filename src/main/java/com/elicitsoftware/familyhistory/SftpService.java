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
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
 *   <li><strong>family.history.sftp.password:</strong> SFTP password for authentication</li>
 *   <li><strong>family.history.sftp.path:</strong> Remote directory path for file uploads</li>
 *   <li><strong>family.history.sftp.port:</strong> SFTP server port (default: 22)</li>
 *   <li><strong>family.history.sftp.timeout:</strong> Connection timeout in milliseconds (default: 30000)</li>
 * </ul>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 1.0
 */
@ApplicationScoped
public class SftpService {

    private static final Logger LOG = LoggerFactory.getLogger(SftpService.class);
    
    @ConfigProperty(name = "family.history.sftp.host")
    String sftpHost;
    
    @ConfigProperty(name = "family.history.sftp.username")
    String sftpUsername;
    
    @ConfigProperty(name = "family.history.sftp.password")
    String sftpPassword;
    
    @ConfigProperty(name = "family.history.sftp.path")
    String sftpPath;
    
    @ConfigProperty(name = "family.history.sftp.port", defaultValue = "22")
    int sftpPort;
    
    @ConfigProperty(name = "family.history.sftp.timeout", defaultValue = "30000")
    int sftpTimeout;
    
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
            LOG.debug("Connecting to SFTP server {}:{} for file upload: {}", sftpHost, sftpPort, fileName);
            
            // Create JSch session
            JSch jsch = new JSch();
            session = jsch.getSession(sftpUsername, sftpHost, sftpPort);
            session.setPassword(sftpPassword);
            
            // Disable strict host key checking for Docker environment
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(sftpTimeout);
            
            // Connect to the session
            session.connect();
            LOG.debug("SSH session connected to {}:{}", sftpHost, sftpPort);
            
            // Open SFTP channel
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            LOG.debug("SFTP channel opened successfully");
            
            // Ensure the remote directory exists
            ensureRemoteDirectoryExists(sftpChannel, sftpPath);
            
            // Change to the target directory
            sftpChannel.cd(sftpPath);
            
            // Upload the file
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData)) {
                sftpChannel.put(inputStream, fileName);
            }
            
            LOG.info("Successfully uploaded file: {} ({} bytes) to SFTP server {}:{}{}", 
                     fileName, fileData.length, sftpHost, sftpPort, sftpPath);
            
        } catch (JSchException e) {
            LOG.error("SFTP connection failed for file {}: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to connect to SFTP server", e);
        } catch (SftpException e) {
            LOG.error("SFTP operation failed for file {}: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to SFTP server", e);
        } catch (IOException e) {
            LOG.error("I/O error during file upload {}: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("I/O error during file upload", e);
        } finally {
            // Clean up resources
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
                LOG.debug("SFTP channel disconnected");
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
                LOG.debug("SSH session disconnected");
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
            LOG.debug("Remote directory exists: {}", remotePath);
        } catch (SftpException e) {
            // Directory doesn't exist, create it
            LOG.debug("Creating remote directory: {}", remotePath);
            
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
                    LOG.debug("Created remote directory: {}", dirToCreate);
                    sftpChannel.cd(dirToCreate);
                }
            }
        }
    }
    
    /**
     * Tests the SFTP connection with the configured settings.
     * 
     * <p>Establishes a connection to the SFTP server, verifies access to the
     * configured directory, and tests write permissions by creating and deleting
     * a temporary test file.</p>
     * 
     * @return true if connection is successful and directory is writable, false otherwise
     */
    public boolean testConnection() {
        Session session = null;
        ChannelSftp sftpChannel = null;
        
        try {
            LOG.debug("Testing SFTP connection to {}:{}", sftpHost, sftpPort);
            
            // Create JSch session
            JSch jsch = new JSch();
            session = jsch.getSession(sftpUsername, sftpHost, sftpPort);
            session.setPassword(sftpPassword);
            
            // Disable strict host key checking for Docker environment
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(sftpTimeout);
            
            // Connect to the session
            session.connect();
            LOG.debug("SSH session connected for connection test");
            
            // Open SFTP channel
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            LOG.debug("SFTP channel opened for connection test");
            
            // Ensure the remote directory exists
            ensureRemoteDirectoryExists(sftpChannel, sftpPath);
            
            // Change to the target directory
            sftpChannel.cd(sftpPath);
            
            // Test write access by creating a temporary file
            String testFileName = ".connection_test_" + System.currentTimeMillis();
            try (ByteArrayInputStream testData = new ByteArrayInputStream("test".getBytes())) {
                sftpChannel.put(testData, testFileName);
            }
            
            // Verify the file exists and then delete it
            sftpChannel.rm(testFileName);
            
            LOG.info("SFTP connection test successful to {}:{}{}", sftpHost, sftpPort, sftpPath);
            return true;
            
        } catch (JSchException e) {
            LOG.error("SFTP connection test failed - connection error: {}", e.getMessage(), e);
            return false;
        } catch (SftpException e) {
            LOG.error("SFTP connection test failed - SFTP operation error: {}", e.getMessage(), e);
            return false;
        } catch (IOException e) {
            LOG.error("SFTP connection test failed - I/O error: {}", e.getMessage(), e);
            return false;
        } finally {
            // Clean up resources
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
                LOG.debug("SFTP channel disconnected after connection test");
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
                LOG.debug("SSH session disconnected after connection test");
            }
        }
    }
}
