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

import com.elicitsoftware.model.Family;
import com.elicitsoftware.model.FamilyManager;
import com.elicitsoftware.model.FamilyMember;
import com.elicitsoftware.request.ReportRequest;
import com.elicitsoftware.response.ReportResponse;
import com.elicitsoftware.response.pdf.Content;
import com.elicitsoftware.response.pdf.Style;
import com.elicitsoftware.test.LogCapture;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@code pedigree.Service}, exercising the JAX-RS resource as plain Java
 * (no CDI/Quarkus bootstrap - there's no reachable Postgres in this environment). Private
 * helper methods are invoked via reflection since they contain non-trivial logic that isn't
 * otherwise reachable without going through {@code report()}'s outbound HTTP call.
 * {@code report()} itself is exercised against a local stand-in for the pedigree-drawing
 * service (JDK {@code HttpServer}), which is how UC-003 A1, BR-001 and BR-002 are checked.
 */
class ServiceTest {

    private Service newService(FamilyManager familyManager) {
        Service service = new Service();
        service.familyManager = familyManager;
        return service;
    }

    private static Object invokePrivate(Service service, String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = Service.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(service, args);
    }

    // ------------------------------------------------------------------
    // resolvePort(URI) - private
    // ------------------------------------------------------------------

    @Test
    void resolvePort_explicitPort_isReturnedDirectly() throws Exception {
        Object result = invokePrivate(newService(null), "resolvePort", new Class<?>[]{URI.class},
                URI.create("http://pedigree-host:9090/svg"));
        assertEquals(9090, result);
    }

    @Test
    void resolvePort_httpsNoExplicitPort_defaultsTo443() throws Exception {
        Object result = invokePrivate(newService(null), "resolvePort", new Class<?>[]{URI.class},
                URI.create("https://pedigree-host/svg"));
        assertEquals(443, result);
    }

    @Test
    void resolvePort_httpNoExplicitPort_defaultsTo80() throws Exception {
        Object result = invokePrivate(newService(null), "resolvePort", new Class<?>[]{URI.class},
                URI.create("http://pedigree-host/svg"));
        assertEquals(80, result);
    }

    @Test
    void resolvePort_unknownScheme_returnsMinusOne() throws Exception {
        Object result = invokePrivate(newService(null), "resolvePort", new Class<?>[]{URI.class},
                URI.create("ftp://pedigree-host/svg"));
        assertEquals(-1, result);
    }

    // ------------------------------------------------------------------
    // isValidSvg(String) - private
    // ------------------------------------------------------------------

    @Test
    void isValidSvg_null_isFalse() throws Exception {
        assertFalse((Boolean) invokePrivate(newService(null), "isValidSvg", new Class<?>[]{String.class}, new Object[]{null}));
    }

    @Test
    void isValidSvg_empty_isFalse() throws Exception {
        assertFalse((Boolean) invokePrivate(newService(null), "isValidSvg", new Class<?>[]{String.class}, ""));
    }

    @Test
    void isValidSvg_xmlDeclaration_isTrue() throws Exception {
        assertTrue((Boolean) invokePrivate(newService(null), "isValidSvg", new Class<?>[]{String.class},
                "<?xml version=\"1.0\"?><svg></svg>"));
    }

    @Test
    void isValidSvg_lowercaseSvgTag_isTrue() throws Exception {
        assertTrue((Boolean) invokePrivate(newService(null), "isValidSvg", new Class<?>[]{String.class},
                "  <svg xmlns='...'></svg>"));
    }

    @Test
    void isValidSvg_uppercaseSvgTag_isTrue() throws Exception {
        assertTrue((Boolean) invokePrivate(newService(null), "isValidSvg", new Class<?>[]{String.class},
                "<SVG></SVG>"));
    }

    @Test
    void isValidSvg_plainErrorMessage_isFalse() throws Exception {
        assertFalse((Boolean) invokePrivate(newService(null), "isValidSvg", new Class<?>[]{String.class},
                "Server returned non-OK status: 500"));
    }

    // ------------------------------------------------------------------
    // buildMultipartBody(String, String, String) - private
    // ------------------------------------------------------------------

    @Test
    void buildMultipartBody_containsBoundaryFieldNameAndValue() throws Exception {
        byte[] bytes = (byte[]) invokePrivate(newService(null), "buildMultipartBody",
                new Class<?>[]{String.class, String.class, String.class},
                "BOUNDARY123", "ped", "famid\tid\tsex\n");

        String body = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(body.contains("--BOUNDARY123\r\n"));
        assertTrue(body.contains("Content-Disposition: form-data; name=\"ped\"; filename=\"ped\"\r\n"));
        assertTrue(body.contains("Content-Type: text/plain\r\n"));
        assertTrue(body.contains("Content-Transfer-Encoding: binary\r\n"));
        assertTrue(body.contains("famid\tid\tsex\n"));
        assertTrue(body.endsWith("--BOUNDARY123--\r\n"));
    }

    // ------------------------------------------------------------------
    // getPDFStyles() - private
    // ------------------------------------------------------------------

    @Test
    void getPDFStyles_definesExpectedNamedStyles() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Style> styles = (Map<String, Style>) invokePrivate(newService(null), "getPDFStyles", new Class<?>[]{});

        assertEquals(4, styles.size());
        Style title = styles.get("ped_title");
        assertNotNull(title);
        assertEquals(16, title.fontSize);
        assertEquals(Boolean.TRUE, title.bold);
        assertEquals("center", title.alignment);

        assertNotNull(styles.get("ped_image"));
        assertEquals("center", styles.get("ped_image").alignment);

        assertEquals("green", styles.get("ped_green").color);
        assertEquals("red", styles.get("ped_red").color);
    }

    // ------------------------------------------------------------------
    // getPDFContent(String, boolean) - private
    // ------------------------------------------------------------------

    @Test
    void getPDFContent_validSvg_noMultipleCancers_hasThreeElementsWithSvgSet() throws Exception {
        String svg = "<svg><rect/></svg>";
        Content[] content = (Content[]) invokePrivate(newService(null), "getPDFContent",
                new Class<?>[]{String.class, boolean.class}, svg, false);

        assertEquals(3, content.length);
        assertEquals(svg, content[0].svg);
        assertNull(content[0].text);
        assertEquals("red fill = family member with cancer", content[1].text);
        assertEquals("ped_red", content[1].style);
    }

    @Test
    void getPDFContent_multipleCancers_appendsFourthDisclaimerElement() throws Exception {
        Content[] content = (Content[]) invokePrivate(newService(null), "getPDFContent",
                new Class<?>[]{String.class, boolean.class}, "<svg></svg>", true);

        assertEquals(4, content.length);
        assertNotNull(content[3].text);
        assertTrue(content[3].text.contains("multiple diagnoses"));
    }

    /** UC-003 BR-001: the placeholder is a fixed sentence; what the service answered is not repeated. */
    @Test
    void getPDFContent_invalidSvg_rendersTheFixedPlaceholderInsteadOfSvg() throws Exception {
        String error = "Server returned non-OK status: 500";
        Content[] content = (Content[]) invokePrivate(newService(null), "getPDFContent",
                new Class<?>[]{String.class, boolean.class}, error, false);

        assertNull(content[0].svg);
        assertEquals(Service.DIAGRAM_UNAVAILABLE, content[0].text);
    }

    @Test
    void getPDFContent_nullSvg_rendersTheFixedPlaceholder() throws Exception {
        Content[] content = (Content[]) invokePrivate(newService(null), "getPDFContent",
                new Class<?>[]{String.class, boolean.class}, new Object[]{null, false});

        assertEquals(Service.DIAGRAM_UNAVAILABLE, content[0].text);
    }

    // ------------------------------------------------------------------
    // report(ReportRequest) against a local stand-in for the drawing service
    // ------------------------------------------------------------------

    private HttpServer drawingService;

    @AfterEach
    void stopDrawingService() {
        if (drawingService != null) {
            drawingService.stop(0);
        }
    }

    /** Starts a local HTTP server answering {@code POST /svg} with the given status and body. */
    private String startDrawingService(int status, String body) throws Exception {
        drawingService = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        drawingService.createContext("/svg", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        drawingService.start();
        return "http://127.0.0.1:" + drawingService.getAddress().getPort() + "/svg";
    }

    private Service reportingService(String pedigreeUrl) throws Exception {
        FamilyManager manager = Mockito.mock(FamilyManager.class);
        Family family = new Family();
        FamilyMember member = new FamilyMember();
        member.ID = 9; // not the proband, whose display name is fixed
        member.name = "Zzyzx-Marker";
        family.family.add(member);
        Mockito.when(manager.getFamily(42L)).thenReturn(family);
        assertTrue(family.toString().contains("Zzyzx-Marker"), "the family table carries the name");

        Service service = newService(manager);
        service.tracer = OpenTelemetry.noop().getTracer("test");
        Field url = Service.class.getDeclaredField("pedigreeURL");
        url.setAccessible(true);
        url.set(service, pedigreeUrl);
        return service;
    }

    private static ReportRequest requestFor(long respondentId) {
        ReportRequest req = new ReportRequest();
        req.id = respondentId;
        return req;
    }

    @Test
    void report_serviceAnswersWithADiagram_returnsItAsHtmlAndPdfContent() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>";
        Service service = reportingService(startDrawingService(200, svg));

        ReportResponse response = service.report(requestFor(42L));

        assertEquals(svg, response.innerHTML);
        assertEquals(svg, response.pdf.content[0].svg);
    }

    /** UC-003 A1, BR-001: a failed call leaves the fixed placeholder and nothing of the answer. */
    @Test
    void report_serviceAnswersWithAnError_showsOnlyTheFixedPlaceholder() throws Exception {
        Service service = reportingService(startDrawingService(500, "internal-detail-marker at pedigree-host"));

        ReportResponse response = service.report(requestFor(42L));

        assertEquals(Service.DIAGRAM_UNAVAILABLE, response.innerHTML);
        assertNull(response.pdf.content[0].svg);
        assertEquals(Service.DIAGRAM_UNAVAILABLE, response.pdf.content[0].text);
    }

    /** UC-003 A1, BR-001: so does a service that cannot be reached. */
    @Test
    void report_serviceCannotBeReached_showsOnlyTheFixedPlaceholder() throws Exception {
        String url = startDrawingService(200, "<svg></svg>");
        drawingService.stop(0);
        Service service = reportingService(url);

        ReportResponse response = service.report(requestFor(42L));

        assertEquals(Service.DIAGRAM_UNAVAILABLE, response.innerHTML);
        assertEquals(Service.DIAGRAM_UNAVAILABLE, response.pdf.content[0].text);
        assertFalse(response.innerHTML.contains("127.0.0.1"));
    }

    /** UC-003 BR-002, NFR-009: the family table is sent, not logged; its size is. */
    @Test
    void report_logsTheSizeOfTheFamilyTableAndNotItsContent() throws Exception {
        Service service = reportingService(startDrawingService(200, "<svg></svg>"));

        try (LogCapture log = LogCapture.start()) {
            service.report(requestFor(42L));

            assertFalse(log.contains("Zzyzx-Marker"), log.text());
            assertFalse(log.contains("famid"), log.text());
            assertTrue(log.contains("1 rows"), log.text());
        }
    }
}
