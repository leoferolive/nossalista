package br.com.leoferolive.nossalista.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/** Guarda o authorization-request OAuth2 num envelope assinado sem HttpSession. */
@Component
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "nl_oauth2_request";
    static final String PRODUCTION_COOKIE_NAME = "__Host-nl_oauth2_request";
    static final int TTL_SECONDS = 180;
    private static final int MAX_COOKIE_VALUE_LENGTH = 4096;
    private static final String DEFAULT_TEST_SIGNING_KEY =
        "test-oauth2-request-signing-key-minimum-32-bytes";

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final byte[] signingKey;
    private final byte[] previousSigningKey;
    private final String cookieName;
    private final boolean secure;

    @Autowired
    public CookieOAuth2AuthorizationRequestRepository(
        Environment environment,
        @Value("${app.auth.oauth2-request-cookie.signing-key}") String signingKey,
        @Value("${app.auth.oauth2-request-cookie.previous-signing-key:}") String previousSigningKey,
        @Value("${jwt.secret}") String jwtSecret
    ) {
        this(environment, signingKey, previousSigningKey, jwtSecret, Clock.systemUTC());
    }

    /** Constructor used by focused unit tests without a Spring context. */
    CookieOAuth2AuthorizationRequestRepository() {
        this(null, DEFAULT_TEST_SIGNING_KEY, "", "different-jwt-secret", Clock.systemUTC());
    }

    CookieOAuth2AuthorizationRequestRepository(
        Environment environment, String signingKey, String previousSigningKey,
        String jwtSecret, Clock clock
    ) {
        validateKeys(signingKey, previousSigningKey, jwtSecret);
        this.clock = clock;
        this.signingKey = signingKey.getBytes(StandardCharsets.UTF_8);
        this.previousSigningKey = previousSigningKey == null || previousSigningKey.isBlank()
            ? null : previousSigningKey.getBytes(StandardCharsets.UTF_8);
        boolean productionProfile = environment != null
            && Arrays.asList(environment.getActiveProfiles()).contains("prod");
        String configuredName = environment == null ? null
            : environment.getProperty("app.auth.oauth2-request-cookie.name");
        if (configuredName == null) {
            configuredName = environment == null ? COOKIE_NAME
                : environment.getProperty("app.auth.session-cookie.name",
                    productionProfile ? PRODUCTION_COOKIE_NAME : COOKIE_NAME)
                    .startsWith("__Host-") ? PRODUCTION_COOKIE_NAME : COOKIE_NAME;
        }
        boolean configuredSecure = environment != null
            ? Boolean.parseBoolean(environment.getProperty(
                "app.auth.oauth2-request-cookie.secure", Boolean.toString(productionProfile)))
            : false;
        validateCookieConfiguration(productionProfile, configuredName, configuredSecure);
        this.cookieName = configuredName;
        this.secure = configuredSecure;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModules(
            SecurityJackson2Modules.getModules(getClass().getClassLoader()));
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request).flatMap(this::deserialize).orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            clearCookie(response);
            return;
        }
        String envelope = serialize(authorizationRequest);
        if (envelope.length() > MAX_COOKIE_VALUE_LENGTH) {
            throw new IllegalStateException("OAuth2 authorization-request excede o limite do cookie");
        }
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(envelope, TTL_SECONDS).toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        Optional<String> rawCookie = readCookie(request);
        OAuth2AuthorizationRequest authorizationRequest = rawCookie.flatMap(this::deserialize).orElse(null);
        if (rawCookie.isPresent()) {
            clearCookie(response);
        }
        return authorizationRequest;
    }

    private ResponseCookie buildCookie(String value, int maxAgeSeconds) {
        return ResponseCookie.from(cookieName, value)
            .path("/")
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .maxAge(maxAgeSeconds)
            .build();
    }

    private void clearCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", 0).toString());
    }

    private Optional<String> readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
            .filter(cookie -> cookieName.equals(cookie.getName()))
            .map(Cookie::getValue)
            .filter(value -> value != null && !value.isBlank())
            .findFirst();
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            long issuedAt = clock.instant().getEpochSecond();
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("iat", issuedAt);
            payload.put("exp", issuedAt + TTL_SECONDS);
            payload.set("request", objectMapper.valueToTree(authorizationRequest));
            byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
            return encode(payloadBytes) + "." + encode(sign(payloadBytes, signingKey));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar OAuth2AuthorizationRequest", e);
        }
    }

    private Optional<OAuth2AuthorizationRequest> deserialize(String envelope) {
        try {
            if (envelope.length() > MAX_COOKIE_VALUE_LENGTH) {
                return Optional.empty();
            }
            String[] parts = envelope.split("\\.", -1);
            if (parts.length != 2 || !isValidSignature(parts[0], parts[1])) {
                return Optional.empty();
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            JsonNode payload = objectMapper.readTree(payloadBytes);
            long now = clock.instant().getEpochSecond();
            long issuedAt = payload.path("iat").asLong(Long.MIN_VALUE);
            long expiresAt = payload.path("exp").asLong(Long.MIN_VALUE);
            if (issuedAt > now || expiresAt <= now || expiresAt - issuedAt > TTL_SECONDS) {
                return Optional.empty();
            }
            JsonNode request = payload.get("request");
            if (request == null || request.isNull()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.treeToValue(request, OAuth2AuthorizationRequest.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private boolean isValidSignature(String payloadPart, String signaturePart) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(payloadPart);
            byte[] received = Base64.getUrlDecoder().decode(signaturePart);
            if (MessageDigest.isEqual(received, sign(payload, signingKey))) {
                return true;
            }
            return previousSigningKey != null
                && MessageDigest.isEqual(received, sign(payload, previousSigningKey));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] sign(byte[] payload, byte[] key) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA-256 indisponível", e);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static void validateKeys(String signingKey, String previousSigningKey, String jwtSecret) {
        validateKeyLength("OAUTH2_REQUEST_SIGNING_KEY", signingKey);
        if (signingKey.equals(jwtSecret)) {
            throw new IllegalStateException("OAUTH2_REQUEST_SIGNING_KEY deve ser distinta de JWT_SECRET");
        }
        if (previousSigningKey != null && !previousSigningKey.isBlank()) {
            validateKeyLength("OAUTH2_REQUEST_SIGNING_KEY_PREVIOUS", previousSigningKey);
            if (previousSigningKey.equals(signingKey) || previousSigningKey.equals(jwtSecret)) {
                throw new IllegalStateException("chaves OAuth2 anteriores devem ser distintas das chaves atuais");
            }
        }
    }

    private static void validateKeyLength(String name, String value) {
        if (value == null || value.isBlank()
            || value.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(name + " ausente ou curto demais: mínimo de 32 bytes");
        }
    }

    private static void validateCookieConfiguration(boolean production, String name, boolean secure) {
        if (name.startsWith("__Host-") && !secure) {
            throw new IllegalStateException("Cookies com prefixo __Host- exigem Secure=true");
        }
        if (production && (!PRODUCTION_COOKIE_NAME.equals(name) || !secure)) {
            throw new IllegalStateException(
                "O profile prod exige cookie OAuth2 __Host-nl_oauth2_request com Secure=true");
        }
        if (!production && (PRODUCTION_COOKIE_NAME.equals(name) || secure)) {
            throw new IllegalStateException(
                "Profiles dev/test exigem cookie OAuth2 nl_oauth2_request com Secure=false");
        }
    }
}
