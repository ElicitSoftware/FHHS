package com.elicitsoftware.common.security;

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

import com.elicitsoftware.test.LogCapture;
import com.elicitsoftware.test.ServiceCalls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-006: the configured service token - its startup checks (A3 to A6, BR-007), the comparison
 * (BR-003), the trusted-origin rule for outbound calls (BR-006) and that it is never written
 * anywhere (BR-004). Plain Java; the bean is built the way CDI builds it.
 */
class ServiceTokenTest {

    private static final String PEDIGREE = "http://pedigree:8000/svg";

    private static ServiceToken token(String value, String origins) {
        return new ServiceToken(Optional.ofNullable(value), Optional.ofNullable(origins), Optional.of(PEDIGREE));
    }

    private static boolean sentTo(ServiceToken token, String address) {
        URI uri;
        try {
            uri = new URI(address);
        } catch (java.net.URISyntaxException e) {
            return false; // an address that is not a URI is never called, so nothing is sent
        }
        return token.authorizationFor(uri).isPresent();
    }

    // ------------------------------------------------------------------
    // Startup checks
    // ------------------------------------------------------------------

    /** UC-006 A3: unset or blank is "not configured", not a start failure. */
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void blankTokenIsNotConfigured(String value) {
        ServiceToken token = token(value, null);

        assertFalse(token.isConfigured());
        assertFalse(token.matches(value));
        assertFalse(token.matches(""));
        assertTrue(token.authorization().isEmpty());
        assertTrue(token.authorizationFor(URI.create(PEDIGREE)).isEmpty());
    }

    /** UC-006 A3: so is an absent property. */
    @Test
    void absentTokenIsNotConfigured() {
        assertFalse(token(null, null).isConfigured());
        assertFalse(token(null, null).matches(null));
    }

    /** UC-006 A4, BR-007: a short token fails the start; the message names the variable and the length required only. */
    @Test
    void shortTokenFailsTheStartWithoutDisclosingIt() {
        String tooShort = "only-31-characters-long-abcdefg";
        assertEquals(31, tooShort.length());

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> token(tooShort, null));

        assertTrue(failure.getMessage().contains("ELICIT_SERVICE_TOKEN"), failure.getMessage());
        assertTrue(failure.getMessage().contains("at least 32 characters"), failure.getMessage());
        assertFalse(failure.getMessage().contains(tooShort), "the value is never printed");
        assertFalse(failure.getMessage().contains("31"), "nor its length");
    }

    /** UC-006 BR-007: 32 characters is enough. */
    @Test
    void tokenOfTheMinimumLengthIsAccepted() {
        String exact = "exactly-32-characters-long-abcde";
        assertEquals(ServiceToken.MIN_LENGTH, exact.length());

        assertTrue(token(exact, null).isConfigured());
    }

    /** UC-006 A3: one warning that names the variable. */
    @Test
    void startupWarnsOnceWhenTheTokenIsNotSet() {
        try (LogCapture log = LogCapture.start()) {
            token(null, null).onStart(null);

            assertEquals(1, log.messages().size(), log.text());
            assertTrue(log.contains("ELICIT_SERVICE_TOKEN is not set"), log.text());
        }
    }

    /** UC-006 A6: the development value is accepted and warned about, without being printed. */
    @Test
    void startupWarnsAboutTheDevelopmentToken() {
        try (LogCapture log = LogCapture.start()) {
            token(ServiceToken.DEVELOPMENT_TOKEN, null).onStart(null);

            assertEquals(1, log.messages().size(), log.text());
            assertTrue(log.contains("development value"), log.text());
            assertFalse(log.contains(ServiceToken.DEVELOPMENT_TOKEN), log.text());
        }
    }

    /** UC-006 BR-004: a site's own token starts silently. */
    @Test
    void startupLogsNothingForAConfiguredToken() {
        try (LogCapture log = LogCapture.start()) {
            ServiceCalls.serviceToken("http://fhhs:8080").onStart(null);

            assertTrue(log.messages().isEmpty(), log.text());
        }
    }

    /** UC-006 BR-004: nothing that prints the bean prints the token. */
    @Test
    void toStringNeverCarriesTheToken() {
        assertEquals("ServiceToken[set]", ServiceCalls.serviceToken().toString());
        assertEquals("ServiceToken[not set]", ServiceCalls.serviceTokenNotConfigured().toString());
    }

    // ------------------------------------------------------------------
    // Comparison (BR-003)
    // ------------------------------------------------------------------

    @Test
    void matchesOnlyTheConfiguredValue() {
        ServiceToken token = ServiceCalls.serviceToken();

        assertTrue(token.matches(ServiceCalls.TOKEN));
        assertFalse(token.matches(ServiceCalls.TOKEN + "x"), "longer");
        assertFalse(token.matches(ServiceCalls.TOKEN.substring(1)), "shorter");
        assertFalse(token.matches(ServiceCalls.TOKEN.toUpperCase()), "another case");
        assertFalse(token.matches(""));
        assertFalse(token.matches(null));
    }

    /** The configured value is stripped, as an environment file with a stray space would need. */
    @Test
    void surroundingWhitespaceIsNotPartOfTheToken() {
        assertTrue(token("  " + ServiceCalls.TOKEN + "\n", null).matches(ServiceCalls.TOKEN));
    }

    // ------------------------------------------------------------------
    // Trusted origins (BR-006)
    // ------------------------------------------------------------------

    /** The origin of pedigree.url is trusted without being listed; a listed origin is too. */
    @Test
    void trustedOriginsAreTheListedOnesAndThePedigreeService() {
        ServiceToken token = token(ServiceCalls.TOKEN, "http://fhhs:8080, https://Reports.Example.org");

        assertEquals(java.util.Set.of("http://pedigree:8000", "http://fhhs:8080", "https://reports.example.org:443"),
                token.trustedOrigins());
        assertEquals("Bearer " + ServiceCalls.TOKEN, token.authorizationFor(URI.create(PEDIGREE)).orElseThrow());
        assertEquals("Bearer " + ServiceCalls.TOKEN, token.authorization().orElseThrow());
    }

    /** Scheme and host compare in lower case, the port by its effective value, and the path does not matter. */
    @ParameterizedTest
    @ValueSource(strings = {
            "http://fhhs:8080/proband/report",
            "HTTP://FHHS:8080/casummary/report?x=1",
            "http://fhhs:8080",
            "http://web/pedigree/report",
            "http://web:80/pedigree/report",
            "https://secure/report",
            "https://secure:443/report"})
    void sendsTheTokenToATrustedOrigin(String address) {
        ServiceToken token = token(ServiceCalls.TOKEN, "http://fhhs:8080,http://web,https://secure:443");

        assertTrue(sentTo(token, address), address);
    }

    /**
     * Equality of origins and nothing looser: no prefix, suffix or wildcard, no other scheme or
     * port, never an address with user information or without a host.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "http://fhhs:8080@evil/",
            "http://fhhs:8080@evil:8080/proband/report",
            "http://user:secret@fhhs:8080/proband/report",
            "http://user@fhhs:8080/",
            "http://fhhs:8080.evil/",
            "http://fhhs.evil:8080/",
            "http://evil.fhhs:8080/",
            "http://fhhs:8080evil/",
            "http://fhhs:80800/",
            "http://fhhs:808/",
            "http://fhhs/",
            "https://fhhs:8080/",
            "http://fhhs.:8080/",
            "http://evil/?http://fhhs:8080/",
            "http://evil/http://fhhs:8080/",
            "http://evil#@fhhs:8080/",
            "http://fhhs:8080\\@evil/",
            "http://evil\\@fhhs:8080/",
            "http://fhhs%3A8080/",
            "http://fhhs:8080%40evil/",
            "http:fhhs:8080",
            "http:///proband/report",
            "//fhhs:8080/proband/report",
            "fhhs:8080/proband/report",
            "/proband/report",
            "ftp://fhhs:8080/",
            "file:///etc/passwd",
            "http://[::1]:8080/",
            "http://127.0.0.1:8080/",
            "http://localhost:8080/",
            ""})
    void neverSendsTheTokenElsewhere(String address) {
        ServiceToken token = token(ServiceCalls.TOKEN, "http://fhhs:8080");

        assertFalse(sentTo(token, address), address);
    }

    /** A null address is no destination. */
    @Test
    void nullAddressGetsNoToken() {
        assertTrue(ServiceCalls.serviceToken().authorizationFor(null).isEmpty());
    }

    /** UC-006 A5: an entry that is not a bare origin fails the start and is named. */
    @ParameterizedTest
    @ValueSource(strings = {
            "fhhs:8080",
            "fhhs",
            "http://",
            "ftp://fhhs:8080",
            "http://fhhs:8080/",
            "http://fhhs:8080/proband/report",
            "http://fhhs:8080?x=1",
            "http://fhhs:8080#top",
            "http://user@fhhs:8080",
            "http://*.example.org",
            "http://fhhs:8080 http://other:8080",
            "*"})
    void trustedOriginEntryThatIsNotAnOriginFailsTheStart(String entry) {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> token(ServiceCalls.TOKEN, "http://good:8080," + entry));

        assertTrue(failure.getMessage().contains("ELICIT_SERVICE_TRUSTED_ORIGINS"), failure.getMessage());
        assertTrue(failure.getMessage().contains("\"" + entry + "\""), failure.getMessage());
        assertFalse(failure.getMessage().contains(ServiceCalls.TOKEN));
    }

    /** Empty entries (a trailing comma, an unset variable) are not entries. */
    @Test
    void emptyTrustedOriginEntriesAreIgnored() {
        assertEquals(java.util.Set.of("http://pedigree:8000", "http://fhhs:8080"),
                token(ServiceCalls.TOKEN, " ,http://fhhs:8080, ,").trustedOrigins());
        assertEquals(java.util.Set.of("http://pedigree:8000"), token(ServiceCalls.TOKEN, "").trustedOrigins());
    }

    /** A pedigree.url without an http origin adds nothing, says so, and does not stop the start. */
    @Test
    void pedigreeUrlWithoutAnOriginAddsNothing() {
        ServiceToken token = new ServiceToken(Optional.of(ServiceCalls.TOKEN), Optional.empty(), Optional.of("not a url"));

        assertTrue(token.trustedOrigins().isEmpty());
        try (LogCapture log = LogCapture.start()) {
            token.onStart(null);
            assertTrue(log.contains("pedigree.url"), log.text());
        }
        assertTrue(new ServiceToken(Optional.of(ServiceCalls.TOKEN), Optional.empty(), Optional.empty())
                .trustedOrigins().isEmpty());
    }
}
