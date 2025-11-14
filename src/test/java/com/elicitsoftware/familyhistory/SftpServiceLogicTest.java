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
import java.io.File;
import java.util.Optional;

/**
 * Test to mimic the exact logic in SftpService
 */
public class SftpServiceLogicTest {
    
    public static void main(String[] args) {
        // Mimic the configuration from application.properties
        Optional<String> sftpPassword = Optional.empty(); // Not using password
        Optional<String> sftpPrivateKey = Optional.of("../sftp_ssh/fhhs_user");
        String sftpHost = "localhost";
        String sftpUsername = "fhhs_user";
        int sftpPort = 22;
        int sftpTimeout = 30000;
        
        System.out.println("Testing SftpService logic for SSH key authentication...");
        
        try {
            Session session = createAuthenticatedSession(
                sftpHost, sftpUsername, sftpPort, sftpTimeout, 
                sftpPassword, sftpPrivateKey
            );
            
            session.connect();
            System.out.println("✅ SSH session connected successfully!");
            
            // Open SFTP channel
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            System.out.println("✅ SFTP channel opened successfully!");
            
            // Test directory operations
            sftpChannel.cd("/upload/reports");
            System.out.println("✓ Changed to directory: /upload/reports");
            
            System.out.println("🎉 SftpService logic test PASSED!");
            
            // Clean up
            sftpChannel.disconnect();
            session.disconnect();
            
        } catch (Exception e) {
            System.err.println("❌ Test FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Exact copy of the createAuthenticatedSession method from SftpService
     */
    private static Session createAuthenticatedSession(
            String sftpHost, String sftpUsername, int sftpPort, int sftpTimeout,
            Optional<String> sftpPassword, Optional<String> sftpPrivateKey) throws JSchException {
        
        JSch jsch = new JSch();
        Session session = jsch.getSession(sftpUsername, sftpHost, sftpPort);
        
        // Configure authentication method
        if (sftpPrivateKey.isPresent() && !sftpPrivateKey.get().trim().isEmpty()) {
            // Use SSH key authentication
            String privateKeyPath = sftpPrivateKey.get().trim();
            System.out.println("Using SSH key authentication with key: " + privateKeyPath);
            
            try {
                // Check if the path is relative and make it absolute if needed
                File keyFile = new File(privateKeyPath);
                if (!keyFile.isAbsolute()) {
                    // Make path relative to the application working directory
                    keyFile = new File(System.getProperty("user.dir"), privateKeyPath);
                }
                
                if (!keyFile.exists()) {
                    throw new JSchException("Private key file not found: " + keyFile.getAbsolutePath());
                }
                
                jsch.addIdentity(keyFile.getAbsolutePath());
                System.out.println("Successfully loaded SSH private key from: " + keyFile.getAbsolutePath());
                
            } catch (JSchException e) {
                System.err.println("Failed to load SSH private key from " + privateKeyPath + ": " + e.getMessage());
                throw e;
            }
            
        } else if (sftpPassword.isPresent() && !sftpPassword.get().trim().isEmpty()) {
            // Use password authentication
            System.out.println("Using password authentication");
            session.setPassword(sftpPassword.get());
            
        } else {
            throw new JSchException("No authentication method configured. Please set either family.history.sftp.password or family.history.sftp.privateKey");
        }
        
        // Disable strict host key checking for Docker environment
        session.setConfig("StrictHostKeyChecking", "no");
        session.setTimeout(sftpTimeout);
        
        return session;
    }
}
