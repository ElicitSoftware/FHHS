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

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The platform service token (UC-006): the one credential the modules of a deployment present
 * to each other as {@code Authorization: Bearer <token>}.
 *
 * <p>This bean holds the configured value and answers the three questions the rest of FHHS
 * asks of it: is a token configured, does a presented value equal it (BR-003), and may it be
 * sent to a given address (BR-006). The value itself never leaves this class except as the
 * header of an outbound call, and is written to no log, exception or {@code toString()}
 * (BR-004).</p>
 */
@ApplicationScoped
public class ServiceToken {

    /** The environment variable an operator sets; named in every message. */
    public static final String VARIABLE = "ELICIT_SERVICE_TOKEN";

    /** The environment variable that lists the trusted origins. */
    public static final String ORIGINS_VARIABLE = "ELICIT_SERVICE_TRUSTED_ORIGINS";

    /** UC-006 BR-007: the shortest token the application starts with. */
    public static final int MIN_LENGTH = 32;

    /** UC-006 A6: the value the compose stack and the dev profile supply; never for a real site. */
    static final String DEVELOPMENT_TOKEN = "elicit-dev-service-token-not-for-a-real-site";

    /** What the readiness check and the startup warning say when no token is configured. */
    public static final String NOT_SET = VARIABLE + " is not set";

    /** What a call that cannot be made, or a request that cannot be checked, is answered with. */
    public static final String NOT_CONFIGURED = "service token not configured";

    private final String token;
    private final byte[] digest;
    private final Set<String> trustedOrigins;
    private final String unusablePedigreeUrl;

    /**
     * Reads and checks the configuration. Runs at startup (see {@link #onStart}), so a token
     * that is too short or a trusted-origin entry that is not an origin fails the start.
     *
     * @param token          {@code elicit.service.token}; empty or blank means not configured
     * @param trustedOrigins {@code elicit.service.trusted-origins}, comma-separated origins
     * @param pedigreeUrl    {@code pedigree.url}, whose origin is trusted implicitly
     * @throws IllegalStateException for a token under {@link #MIN_LENGTH} characters (the
     *                               message carries neither the value nor its length) or an
     *                               entry that is not {@code scheme://host[:port]}
     */
    @Inject
    public ServiceToken(@ConfigProperty(name = "elicit.service.token") Optional<String> token,
                        @ConfigProperty(name = "elicit.service.trusted-origins") Optional<String> trustedOrigins,
                        @ConfigProperty(name = "pedigree.url") Optional<String> pedigreeUrl) {
        String value = token.map(String::strip).orElse("");
        if (!value.isEmpty() && value.length() < MIN_LENGTH) {
            throw new IllegalStateException(VARIABLE + " (elicit.service.token) must be at least " + MIN_LENGTH
                    + " characters long. Generate one with: openssl rand -hex 32");
        }
        this.token = value.isEmpty() ? null : value;
        this.digest = this.token == null ? null : sha256(this.token);

        Set<String> origins = new LinkedHashSet<>();
        for (String entry : trustedOrigins.orElse("").split(",")) {
            if (!entry.isBlank()) {
                origins.add(parseEntry(entry.strip()));
            }
        }
        // pedigree.url is deployment configuration, so its origin is trusted without being listed.
        String url = pedigreeUrl.map(String::strip).orElse("");
        Optional<String> pedigreeOrigin = url.isEmpty() ? Optional.empty() : originOf(parse(url));
        pedigreeOrigin.ifPresent(origins::add);
        this.unusablePedigreeUrl = !url.isEmpty() && pedigreeOrigin.isEmpty() ? url : null;
        this.trustedOrigins = Set.copyOf(origins);
    }

    /** Whether a token is configured. Without one FHHS refuses requests and makes no service call. */
    public boolean isConfigured() {
        return token != null;
    }

    /**
     * UC-006 BR-003: whether the presented value equals the configured token, compared in
     * constant time over the SHA-256 digests of both, which also hides the token's length.
     *
     * @param presented the value a caller sent after {@code Bearer}; may be {@code null}
     * @return {@code false} when no token is configured or the values differ
     */
    public boolean matches(String presented) {
        if (digest == null || presented == null) {
            return false;
        }
        return MessageDigest.isEqual(digest, sha256(presented));
    }

    /**
     * The {@code Authorization} header value for a destination that deployment configuration
     * names ({@code pedigree.url}); empty when no token is configured.
     */
    public Optional<String> authorization() {
        return Optional.ofNullable(token).map(t -> "Bearer " + t);
    }

    /**
     * UC-006 BR-006: the {@code Authorization} header value for a call to the given address,
     * or empty when the token must not be sent there.
     *
     * <p>The address is the final one, after any resolution. It receives the token only when
     * its origin (lower-case scheme and host, effective port) equals a trusted origin. An
     * address with user information, without a host, or with a scheme other than
     * {@code http}/{@code https} never does; nothing is resolved and nothing is matched by
     * prefix or suffix.</p>
     *
     * @param destination the address about to be called
     * @return the header value, or empty when no token is configured or the origin is not trusted
     */
    public Optional<String> authorizationFor(URI destination) {
        if (token == null) {
            return Optional.empty();
        }
        return originOf(destination).filter(trustedOrigins::contains).flatMap(origin -> authorization());
    }

    /** The trusted origins, each as {@code scheme://host:port}. Configuration, not a secret. */
    public Set<String> trustedOrigins() {
        return trustedOrigins;
    }

    /**
     * Reduces an address to its origin, {@code scheme://host:port} in lower case with the
     * effective port, or empty when it has none that may be compared (BR-006).
     */
    public static Optional<String> originOf(URI uri) {
        if (uri == null || uri.isOpaque() || uri.getScheme() == null || uri.getHost() == null
                || uri.getRawUserInfo() != null) {
            return Optional.empty();
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        int defaultPort;
        if (scheme.equals("http")) {
            defaultPort = 80;
        } else if (scheme.equals("https")) {
            defaultPort = 443;
        } else {
            return Optional.empty();
        }
        int port = uri.getPort() == -1 ? defaultPort : uri.getPort();
        return Optional.of(scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port);
    }

    /** UC-006 A5: one entry of the trusted-origin list, or a start failure that names it. */
    private static String parseEntry(String entry) {
        URI uri = parse(entry);
        Optional<String> origin = originOf(uri);
        boolean bare = uri != null && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                && uri.getRawQuery() == null && uri.getRawFragment() == null;
        if (origin.isEmpty() || !bare) {
            throw new IllegalStateException(ORIGINS_VARIABLE + " (elicit.service.trusted-origins) entry \"" + entry
                    + "\" is not an origin. Write each entry as scheme://host[:port] with http or https and no "
                    + "user information, path, query or fragment, separated by commas.");
        }
        return origin.get();
    }

    private static URI parse(String address) {
        try {
            return new URI(address);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Creates this bean at startup, so its checks fail the start rather than the first request,
     * and writes the warnings an operator must act on (UC-006 A3, A6).
     */
    void onStart(@Observes StartupEvent event) {
        // WARN, not INFO: containers run at WARN, and these are conditions an operator must act on.
        if (token == null) {
            Log.warn(NOT_SET + " (elicit.service.token). This application is not ready: it refuses every "
                    + "request and calls no other service until the deployment's service token is supplied.");
        } else if (token.equals(DEVELOPMENT_TOKEN)) {
            Log.warn(VARIABLE + " is the well-known development value. Replace it before this application "
                    + "serves a real site (openssl rand -hex 32), with the same value for every module.");
        }
        if (unusablePedigreeUrl != null) {
            Log.warn("pedigree.url has no http or https origin, so it adds nothing to the trusted origins.");
        }
    }

    /** Never the token. */
    @Override
    public String toString() {
        return "ServiceToken[" + (token == null ? "not set" : "set") + "]";
    }
}
