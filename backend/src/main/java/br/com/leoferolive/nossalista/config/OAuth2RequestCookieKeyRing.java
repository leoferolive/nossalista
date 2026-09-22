package br.com.leoferolive.nossalista.config;

import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/** Holds the active OAuth request signing key and its explicitly bounded predecessor. */
final class OAuth2RequestCookieKeyRing {

    private static final String RETIREMENT_DEADLINE_PROPERTY =
        "app.auth.oauth2-request-cookie.previous-signing-key-retirement-deadline";
    private final byte[] signingKey;
    private final byte[] previousSigningKey;
    private final Instant previousSigningKeyRetirementDeadline;
    private final Clock clock;

    OAuth2RequestCookieKeyRing(String signingKey, String previousSigningKey,
                               String retirementDeadline, String jwtSecret, Clock clock) {
        validateKeys(signingKey, previousSigningKey, jwtSecret);
        this.clock = clock;
        this.signingKey = toBytes(signingKey);
        this.previousSigningKey = optionalBytes(previousSigningKey);
        this.previousSigningKeyRetirementDeadline = parseRetirementDeadline(previousSigningKey, retirementDeadline);
        requireRetirementWindow(this.previousSigningKeyRetirementDeadline);
    }

    static String configuredRetirementDeadline(Environment environment) {
        return environment == null ? "" : environment.getProperty(RETIREMENT_DEADLINE_PROPERTY, "");
    }

    byte[] sign(byte[] payload) {
        return sign(payload, signingKey);
    }

    boolean matches(byte[] payload, byte[] received) {
        return matches(payload, received, signingKey) || matchesPrevious(payload, received);
    }

    private boolean matchesPrevious(byte[] payload, byte[] received) {
        return previousSigningKey != null
            && clock.instant().isBefore(previousSigningKeyRetirementDeadline)
            && matches(payload, received, previousSigningKey);
    }

    private boolean matches(byte[] payload, byte[] received, byte[] key) {
        return MessageDigest.isEqual(received, sign(payload, key));
    }

    private static byte[] sign(byte[] payload, byte[] key) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA-256 indisponível", e);
        }
    }

    private static void validateKeys(String signingKey, String previousSigningKey, String jwtSecret) {
        validateKeyLength("OAUTH2_REQUEST_SIGNING_KEY", signingKey);
        requireDistinctCurrentKey(signingKey, jwtSecret);
        validatePreviousKey(previousSigningKey, signingKey, jwtSecret);
    }

    private static void requireDistinctCurrentKey(String signingKey, String jwtSecret) {
        if (signingKey.equals(jwtSecret)) {
            throw new IllegalStateException("OAUTH2_REQUEST_SIGNING_KEY deve ser distinta de JWT_SECRET");
        }
    }

    private static void validatePreviousKey(String previousSigningKey, String signingKey, String jwtSecret) {
        if (previousSigningKey == null || previousSigningKey.isBlank()) {
            return;
        }
        validateKeyLength("OAUTH2_REQUEST_SIGNING_KEY_PREVIOUS", previousSigningKey);
        if (previousSigningKey.equals(signingKey) || previousSigningKey.equals(jwtSecret)) {
            throw new IllegalStateException("chaves OAuth2 anteriores devem ser distintas das chaves atuais");
        }
    }

    private static void validateKeyLength(String name, String value) {
        if (value == null || value.isBlank() || value.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(name + " ausente ou curto demais: mínimo de 32 bytes");
        }
    }

    private static Instant parseRetirementDeadline(String previousSigningKey, String configuredDeadline) {
        if (previousSigningKey == null || previousSigningKey.isBlank()) {
            requireAbsentDeadline(configuredDeadline);
            return null;
        }
        requireDeadline(configuredDeadline);
        return parseUtcInstant(configuredDeadline);
    }

    private static void requireAbsentDeadline(String configuredDeadline) {
        if (configuredDeadline != null && !configuredDeadline.isBlank()) {
            throw new IllegalStateException("OAUTH2_REQUEST_SIGNING_KEY_PREVIOUS_RETIREMENT_DEADLINE "
                + "exige OAUTH2_REQUEST_SIGNING_KEY_PREVIOUS");
        }
    }

    private static void requireDeadline(String configuredDeadline) {
        if (configuredDeadline == null || configuredDeadline.isBlank()) {
            throw new IllegalStateException("OAUTH2_REQUEST_SIGNING_KEY_PREVIOUS exige "
                + "OAUTH2_REQUEST_SIGNING_KEY_PREVIOUS_RETIREMENT_DEADLINE em UTC ISO-8601");
        }
    }

    private static Instant parseUtcInstant(String configuredDeadline) {
        if (!configuredDeadline.endsWith("Z")) {
            throw new IllegalStateException("OAUTH2_REQUEST_SIGNING_KEY_PREVIOUS_RETIREMENT_DEADLINE "
                + "deve usar UTC ISO-8601");
        }
        try {
            return Instant.parse(configuredDeadline);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("OAUTH2_REQUEST_SIGNING_KEY_PREVIOUS_RETIREMENT_DEADLINE "
                + "deve usar UTC ISO-8601", e);
        }
    }

    private void requireRetirementWindow(Instant retirementDeadline) {
        if (retirementDeadline != null
            && retirementDeadline.isAfter(clock.instant().plusSeconds(
                CookieOAuth2AuthorizationRequestRepository.TTL_SECONDS))) {
            throw new IllegalStateException("OAUTH2_REQUEST_SIGNING_KEY_PREVIOUS_RETIREMENT_DEADLINE "
                + "não pode exceder a janela de 180 segundos");
        }
    }

    private static byte[] toBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] optionalBytes(String value) {
        return value == null || value.isBlank() ? null : toBytes(value);
    }
}
