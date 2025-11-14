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

/**
 * Standalone test to verify SSH key authentication with JSch
 */
public class SftpStandaloneTest {
    
    public static void main(String[] args) {
        String host = "localhost";
        String username = "fhhs_user";
        String privateKeyPath = "../sftp_ssh/fhhs_user";
        int port = 22;
        
        System.out.println("Testing SFTP SSH key authentication...");
        System.out.println("Host: " + host);
        System.out.println("Username: " + username);
        System.out.println("Private Key: " + privateKeyPath);
        System.out.println("Port: " + port);
        
        Session session = null;
        ChannelSftp sftpChannel = null;
        
        try {
            // Create JSch session
            JSch jsch = new JSch();
            
            // Check if the private key file exists
            File keyFile = new File(privateKeyPath);
            if (!keyFile.exists()) {
                System.err.println("❌ Private key file not found: " + keyFile.getAbsolutePath());
                return;
            }
            
            System.out.println("✓ Private key file found: " + keyFile.getAbsolutePath());
            
            // Add the identity (private key)
            jsch.addIdentity(keyFile.getAbsolutePath());
            System.out.println("✓ Private key loaded successfully");
            
            // Create session
            session = jsch.getSession(username, host, port);
            
            // Disable strict host key checking for test environment
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(30000);
            
            System.out.println("✓ Session created, attempting to connect...");
            
            // Connect to the session
            session.connect();
            System.out.println("✅ SSH session connected successfully!");
            
            // Open SFTP channel
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            System.out.println("✅ SFTP channel opened successfully!");
            
            // Test directory listing
            sftpChannel.cd("/upload/reports");
            System.out.println("✓ Changed to directory: /upload/reports");
            
            java.util.Vector<?> files = sftpChannel.ls(".");
            System.out.println("✓ Directory listing successful, " + files.size() + " items found");
            
            System.out.println("🎉 SSH key authentication test PASSED!");
            
        } catch (JSchException e) {
            System.err.println("❌ JSch Exception: " + e.getMessage());
            e.printStackTrace();
        } catch (SftpException e) {
            System.err.println("❌ SFTP Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
                System.out.println("✓ SFTP channel disconnected");
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
                System.out.println("✓ SSH session disconnected");
            }
        }
    }
}
