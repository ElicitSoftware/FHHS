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
import com.elicitsoftware.response.pdf.Content;
import com.elicitsoftware.response.pdf.Style;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@code pedigree.Service}, exercising the JAX-RS resource as plain Java
 * (no CDI/Quarkus bootstrap - there's no reachable Postgres in this environment). Private
 * helper methods are invoked via reflection since they contain non-trivial logic that isn't
 * otherwise reachable without going through {@code report()}'s real outbound HTTP call.
 * {@code report()} and {@code callPedigree()} are intentionally NOT tested here - they make
 * a real {@code java.net.http.HttpClient} call to an external pedigree-drawing service and
 * are out of scope for a plain unit test.
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
    // getFamily(ReportRequest) - public method, no network call
    // ------------------------------------------------------------------

    @Test
    void getFamily_returnsFamilyToStringFromInjectedManager() {
        FamilyManager manager = Mockito.mock(FamilyManager.class);
        Family family = new Family();
        FamilyMember m = new FamilyMember();
        m.ID = 7;
        m.name = "Respondent";
        family.family.add(m);
        Mockito.when(manager.getFamily(42L)).thenReturn(family);

        ReportRequest req = new ReportRequest();
        req.id = 42L;

        String result = newService(manager).getFamily(req);

        assertEquals(family.toString(), result);
        Mockito.verify(manager).getFamily(42L);
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

    @Test
    void getPDFContent_invalidSvg_rendersErrorTextInsteadOfSvg() throws Exception {
        String error = "Server returned non-OK status: 500";
        Content[] content = (Content[]) invokePrivate(newService(null), "getPDFContent",
                new Class<?>[]{String.class, boolean.class}, error, false);

        assertNull(content[0].svg);
        assertNotNull(content[0].text);
        assertTrue(content[0].text.contains("Pedigree diagram unavailable"));
        assertTrue(content[0].text.contains(error));
    }

    @Test
    void getPDFContent_nullSvg_rendersNoResponseMessage() throws Exception {
        Content[] content = (Content[]) invokePrivate(newService(null), "getPDFContent",
                new Class<?>[]{String.class, boolean.class}, new Object[]{null, false});

        assertTrue(content[0].text.contains("No response from pedigree service"));
    }
}
