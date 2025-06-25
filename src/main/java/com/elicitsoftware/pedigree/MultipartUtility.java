package com.elicitsoftware.pedigree;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for sending multipart/form-data HTTP requests.
 * <p>
 * This class provides functionality to create and send HTTP POST requests
 * with multipart content, specifically designed for uploading files and
 * form data to external services. It's primarily used for communicating
 * with pedigree generation services that require file uploads.
 * </p>
 * <p>
 * The class supports adding both text form fields and file uploads to
 * a single multipart request, making it suitable for complex API interactions.
 * </p>
 *
 * @author Elicit Software
 * @version 1.0
 * @since 2025
 */
public class MultipartUtility {

    /**
     * Unique boundary string used to separate multipart sections.
     */
    private final String boundary;

    /**
     * Line feed constant for proper multipart formatting.
     */
    private static final String LINE_FEED = "\r\n";

    /**
     * HTTP connection for the multipart request.
     */
    private HttpURLConnection httpConn;

    /**
     * Character encoding used for the request.
     */
    private String charset;

    /**
     * Output stream for writing request data.
     */
    private OutputStream outputStream;

    /**
     * Print writer for writing text content to the request.
     */
    private PrintWriter writer;

    /**
     * Constructs a new MultipartUtility for the specified URL and charset.
     * <p>
     * Initializes the HTTP connection with proper headers for multipart
     * form data transmission. Creates a unique boundary based on the
     * current timestamp to separate form sections.
     * </p>
     *
     * @param requestURL the target URL for the multipart request
     * @param charset the character encoding to use for text content
     * @throws IOException if there are issues establishing the HTTP connection
     */
    public MultipartUtility(String requestURL, String charset)
            throws IOException {
        this.charset = charset;

        // creates a unique boundary based on time stamp
        boundary = "===" + System.currentTimeMillis() + "===";

        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
        httpConn.setRequestProperty("User-Agent", "CodeJava Agent");
        httpConn.setRequestProperty("Test", "Bonjour");
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                true);
    }

    /**
     * Adds a text form field to the multipart request.
     * <p>
     * Creates a form-data section with the specified name and value,
     * properly formatted according to multipart/form-data specifications.
     * </p>
     *
     * @param name the name of the form field
     * @param value the text value of the form field
     */
    public void addFormField(String name, String value) {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                .append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=" + charset).append(
                LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a string content as a file upload section to the request.
     * <p>
     * Creates a file upload section using string content rather than
     * an actual file. This is useful for uploading generated content
     * such as pedigree data strings.
     * </p>
     *
     * @param fieldName name attribute for the file upload field
     * @param uploadFile string content to upload as file data
     * @throws IOException if there are issues writing to the output stream
     */
    public void addFilePart(String fieldName, String uploadFile)
            throws IOException {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append(
                "Content-Disposition: form-data; name=\"" + fieldName
                        + "\"; filename=\"" + fieldName + "\"")
                .append(LINE_FEED);
        writer.append(
                "Content-Type: text/plain")
                .append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        byte[] bytes = uploadFile.getBytes();

        outputStream.write(bytes, 0, bytes.length );

        outputStream.flush();

        writer.append(LINE_FEED);
        writer.flush();
    }


    /**
     * Adds a upload file section to the request
     * @param fieldName name attribute for the file upload field
     * @param uploadFile a File to be uploaded
     * @throws IOException if there are issues with file I/O operations
     */
    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append(
                "Content-Disposition: form-data; name=\"" + fieldName
                        + "\"; filename=\"" + fileName + "\"")
                .append(LINE_FEED);
        writer.append(
                "Content-Type: "
                        + URLConnection.guessContentTypeFromName(fileName))
                .append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a header field to the request.
     * @param name - name of the header field
     * @param value - value of the header field
     */
    public void addHeaderField(String name, String value) {
        writer.append(name + ": " + value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException if there are issues with network I/O operations
     */
    public List<String> finish() throws IOException {
        List<String> response = new ArrayList<String>();

        writer.append(LINE_FEED).flush();
        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK || status == 201) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }

        return response;
    }
}
