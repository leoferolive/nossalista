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

import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/** Guarda o authorization-request OAuth2 num envelope assinado sem HttpSession. */
@Component
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
    private final OAuth2RequestCookieKeyRing keyRing;
    private final String cookieName;
    private final boolean secure;

    @Autowired
    public CookieOAuth2AuthorizationRequestRepository(
        Environment environment,
        @Value("${app.auth.oauth2-request-cookie.signing-key}") String signingKey,
        @Value("${app.auth.oauth2-request-cookie.previous-signing-key:}") String previousSigningKey,
        @Value("${app.auth.oauth2-request-cookie.previous-signing-key-retirement-deadline:}")
        String previousSigningKeyRetirementDeadline,
        @Value("${jwt.secret}") String jwtSecret
    ) {
        this(environment, signingKey, previousSigningKey, previousSigningKeyRetirementDeadline,
            jwtSecret, Clock.systemUTC());
    }

    /** Constructor used by focused unit tests without a Spring context. */
    CookieOAuth2AuthorizationRequestRepository() {
        this(null, DEFAULT_TEST_SIGNING_KEY, "", "", "different-jwt-secret", Clock.systemUTC());
    }

    CookieOAuth2AuthorizationRequestRepository(
        Environment environment, String signingKey, String previousSigningKey, String jwtSecret, Clock clock
    ) {
        this(environment, signingKey, previousSigningKey,
            OAuth2RequestCookieKeyRing.configuredRetirementDeadline(environment), jwtSecret, clock);
    }

    private CookieOAuth2AuthorizationRequestRepository(
        Environment environment, String signingKey, String previousSigningKey,
        String previousSigningKeyRetirementDeadline, String jwtSecret, Clock clock
    ) {
        this.clock = clock;
        this.keyRing = new OAuth2RequestCookieKeyRing(signingKey, previousSigningKey,
            previousSigningKeyRetirementDeadline, jwtSecret, clock);
        OAuth2RequestCookieSettings cookieSettings = OAuth2RequestCookieSettings.resolve(environment);
        this.cookieName = cookieSettings.name();
        this.secure = cookieSettings.secure();
        this.objectMapper = createObjectMapper();
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
        requireCookieSize(envelope);
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
            byte[] payload = serializedPayload(authorizationRequest);
            return encode(payload) + "." + encode(keyRing.sign(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar OAuth2AuthorizationRequest", e);
        }
    }

    private byte[] serializedPayload(OAuth2AuthorizationRequest authorizationRequest) throws Exception {
        long issuedAt = clock.instant().getEpochSecond();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("iat", issuedAt);
        payload.put("exp", issuedAt + TTL_SECONDS);
        payload.set("request", objectMapper.valueToTree(authorizationRequest));
        return objectMapper.writeValueAsBytes(payload);
    }

    private Optional<OAuth2AuthorizationRequest> deserialize(String envelope) {
        return verifiedPayload(envelope).flatMap(this::toUnexpiredAuthorizationRequest);
    }

    private Optional<JsonNode> verifiedPayload(String envelope) {
        if (envelope.length() > MAX_COOKIE_VALUE_LENGTH) {
            return Optional.empty();
        }
        String[] parts = envelope.split("\\.", -1);
        if (parts.length != 2 || !isValidSignature(parts[0], parts[1])) {
            return Optional.empty();
        }
        return decodePayload(parts[0]);
    }

    private Optional<JsonNode> decodePayload(String payloadPart) {
        try {
            return Optional.of(objectMapper.readTree(Base64.getUrlDecoder().decode(payloadPart)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<OAuth2AuthorizationRequest> toUnexpiredAuthorizationRequest(JsonNode payload) {
        if (!hasValidLifetime(payload)) {
            return Optional.empty();
        }
        JsonNode request = payload.get("request");
        if (request == null || request.isNull()) {
            return Optional.empty();
        }
        return deserializeAuthorizationRequest(request);
    }

    private Optional<OAuth2AuthorizationRequest> deserializeAuthorizationRequest(JsonNode request) {
        try {
            return Optional.of(objectMapper.treeToValue(request, OAuth2AuthorizationRequest.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private boolean hasValidLifetime(JsonNode payload) {
        long now = clock.instant().getEpochSecond();
        long issuedAt = payload.path("iat").asLong(Long.MIN_VALUE);
        long expiresAt = payload.path("exp").asLong(Long.MIN_VALUE);
        return issuedAt <= now && expiresAt > now && expiresAt - issuedAt <= TTL_SECONDS;
    }

    private boolean isValidSignature(String payloadPart, String signaturePart) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(payloadPart);
            byte[] received = Base64.getUrlDecoder().decode(signaturePart);
            return keyRing.matches(payload, received);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(
            CookieOAuth2AuthorizationRequestRepository.class.getClassLoader()));
        return mapper;
    }


    private static void requireCookieSize(String envelope) {
        if (envelope.length() > MAX_COOKIE_VALUE_LENGTH) {
            throw new IllegalStateException("OAuth2 authorization-request excede o limite do cookie");
        }
    }

}
