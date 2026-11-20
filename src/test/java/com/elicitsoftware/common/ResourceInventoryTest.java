package com.elicitsoftware.common;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * NFR-008: every REST endpoint FHHS serves is the entry point of a use case. The list below is
 * that inventory; an endpoint added to the code without a use case fails here.
 */
class ResourceInventoryTest {

    /** UC-001..UC-004. Liveness and readiness are Quarkus's probes under /q/health, not resources. */
    private static final Set<String> EXPECTED = Set.of(
            "POST /proband/report",
            "POST /casummary/report",
            "POST /pedigree/report",
            "POST /familyhistory/generate");

    @Test
    void everyResourcePathBelongsToAUseCase() throws Exception {
        assertEquals(new TreeSet<>(EXPECTED), servedEndpoints());
    }

    @Test
    void noInspectionEndpointIsServed() throws Exception {
        for (String endpoint : servedEndpoints()) {
            assertFalse(endpoint.contains("/debug"), endpoint);
            assertFalse(endpoint.equals("POST /pedigree/family"), endpoint);
        }
    }

    /**
     * UC-006 BR-008: FHHS has no roles and no security extension that would enforce one, so no
     * class carries a role annotation that says otherwise. The service token filter is the check.
     */
    @Test
    void noClassCarriesARoleAnnotation() throws Exception {
        for (Class<?> type : mainClasses()) {
            assertNoRoleAnnotation(type.getName(), type.getAnnotations());
            for (Method method : type.getDeclaredMethods()) {
                assertNoRoleAnnotation(type.getName() + "#" + method.getName(), method.getAnnotations());
            }
        }
    }

    private static void assertNoRoleAnnotation(String where, java.lang.annotation.Annotation[] annotations) {
        for (java.lang.annotation.Annotation annotation : annotations) {
            assertFalse(annotation.annotationType().getName().startsWith("jakarta.annotation.security."),
                    where + " carries " + annotation.annotationType().getSimpleName());
        }
    }

    /** Every JAX-RS method under {@code com.elicitsoftware} in the compiled main classes. */
    private static Set<String> servedEndpoints() throws IOException, ClassNotFoundException {
        Set<String> endpoints = new TreeSet<>();
        for (Class<?> type : mainClasses()) {
            Path root = type.getAnnotation(Path.class);
            if (root == null || type.isInterface()) {
                continue; // an interface with @Path is a REST client, not something FHHS serves
            }
            for (Method method : type.getDeclaredMethods()) {
                String verb = verb(method);
                if (verb != null) {
                    Path sub = method.getAnnotation(Path.class);
                    endpoints.add(verb + " /" + trim(root.value()) + (sub != null ? "/" + trim(sub.value()) : ""));
                }
            }
        }
        return endpoints;
    }

    /** Every class under {@code com.elicitsoftware} in the compiled main classes. */
    private static List<Class<?>> mainClasses() throws IOException, ClassNotFoundException {
        java.nio.file.Path classes = Paths.get("target", "classes");
        List<Class<?>> types = new ArrayList<>();
        try (Stream<java.nio.file.Path> files = Files.walk(classes.resolve("com/elicitsoftware"))) {
            for (java.nio.file.Path file : (Iterable<java.nio.file.Path>) files.filter(f -> f.toString().endsWith(".class"))::iterator) {
                String name = classes.relativize(file).toString();
                name = name.substring(0, name.length() - ".class".length()).replace('/', '.').replace('\\', '.');
                types.add(Class.forName(name, false, ResourceInventoryTest.class.getClassLoader()));
            }
        }
        return types;
    }

    private static String trim(String path) {
        return path.replaceAll("^/+|/+$", "");
    }

    private static String verb(Method method) {
        if (method.isAnnotationPresent(GET.class)) {
            return "GET";
        }
        if (method.isAnnotationPresent(POST.class)) {
            return "POST";
        }
        if (method.isAnnotationPresent(PUT.class)) {
            return "PUT";
        }
        if (method.isAnnotationPresent(DELETE.class)) {
            return "DELETE";
        }
        return null;
    }
}
