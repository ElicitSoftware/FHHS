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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for {@link MultipartUtility} against a real local HTTP server
 * ({@link HttpServer}, JDK-builtin - no new test dependency needed). MultipartUtility opens
 * a live {@code HttpURLConnection} in its constructor, so a mock can't stand in for the
 * server side; these tests exercise the real request/response wire format instead.
 */
class MultipartUtilityTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpServer startServer(int responseStatus, String responseBody, AtomicReference<String> capturedBody) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/upload", exchange -> handle(exchange, responseStatus, responseBody, capturedBody));
        httpServer.start();
        return httpServer;
    }

    private void handle(HttpExchange exchange, int responseStatus, String responseBody, AtomicReference<String> capturedBody) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            capturedBody.set(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(responseStatus, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void fullRoundTrip_sendsFormFieldAndFilePart_andReturnsResponseLines() throws IOException {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        server = startServer(200, "OK\nresponse-line-2", capturedBody);
        int port = server.getAddress().getPort();

        MultipartUtility utility = new MultipartUtility("http://127.0.0.1:" + port + "/upload", "UTF-8");
        utility.addFormField("field1", "value1");
        utility.addFilePart("file1", "some string content");

        List<String> response = utility.finish();

        assertEquals(List.of("OK", "response-line-2"), response);

        String body = capturedBody.get();
        assertNotNull(body);
        assertTrue(body.contains("Content-Disposition: form-data; name=\"field1\""), body);
        assertTrue(body.contains("value1"), body);
        assertTrue(body.contains("Content-Disposition: form-data; name=\"file1\"; filename=\"file1\""), body);
        assertTrue(body.contains("some string content"), body);
    }

    @Test
    void finish_serverReturnsNonOkStatus_throwsIOExceptionWithStatusCode() throws IOException {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        server = startServer(500, "internal error", capturedBody);
        int port = server.getAddress().getPort();

        MultipartUtility utility = new MultipartUtility("http://127.0.0.1:" + port + "/upload", "UTF-8");
        utility.addFormField("field1", "value1");

        IOException ex = assertThrows(IOException.class, utility::finish);
        assertTrue(ex.getMessage().contains("Server returned non-OK status: 500"), ex.getMessage());
    }

    @Test
    void finish_serverReturns201Created_isTreatedAsSuccess() throws IOException {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        server = startServer(201, "Created", capturedBody);
        int port = server.getAddress().getPort();

        MultipartUtility utility = new MultipartUtility("http://127.0.0.1:" + port + "/upload", "UTF-8");
        utility.addFormField("field1", "value1");

        List<String> response = utility.finish();

        assertEquals(List.of("Created"), response);
    }
}
