package br.com.leoferolive.nossalista.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Guarda o {@link OAuth2AuthorizationRequest} (o {@code state} anti-CSRF + dados
 * do fluxo OAuth2) num COOKIE curto, em vez da {@code HttpSession}.
 *
 * <p><b>Por quê:</b> a aplicação roda com {@code SessionCreationPolicy.STATELESS}
 * (JWT). O repositório padrão do Spring para o authorization-request é
 * {@code HttpSessionOAuth2AuthorizationRequestRepository}, que depende da sessão
 * HTTP — incompatível com STATELESS. Sem ele persistir, a validação de
 * {@code state} no callback do Google fica quebrada/não-determinística e o
 * callback vira re-executável, emitindo múltiplos one-time codes órfãos (o login
 * Google nunca completa). Guardando o authorization-request num cookie próprio,
 * o {@code state} é validado corretamente sem sessão — mantendo a API stateless.</p>
 *
 * <p>O cookie é {@code HttpOnly} + {@code Secure} + {@code SameSite=Lax} (permite
 * o envio no retorno top-level GET do Google) e vive só durante o fluxo (180s).
 * A (de)serialização usa Jackson com os {@link SecurityJackson2Modules} do Spring
 * Security (typing por allowlist), em vez de serialização Java nativa — evitando a
 * brecha de desserialização de objetos arbitrários (CWE-502).</p>
 */
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "OAUTH2_AUTH_REQUEST";
    private static final int TTL_SECONDS = 180;

    /**
     * ObjectMapper com os módulos de segurança do Spring (inclui o mixin de
     * {@code OAuth2AuthorizationRequest}), permitindo (de)serializar o
     * authorization-request com typing restrito a uma allowlist de classes
     * confiáveis — sem serialização Java nativa.
     */
    private final ObjectMapper objectMapper;

    public CookieOAuth2AuthorizationRequestRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModules(
            SecurityJackson2Modules.getModules(getClass().getClassLoader()));
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request).map(this::deserialize).orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            clearCookie(response);
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(serialize(authorizationRequest), TTL_SECONDS).toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            clearCookie(response);
        }
        return authorizationRequest;
    }

    private ResponseCookie buildCookie(String value, int maxAgeSeconds) {
        return ResponseCookie.from(COOKIE_NAME, value)
            .path("/")
            .httpOnly(true)
            .secure(true)
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
            .filter(c -> COOKIE_NAME.equals(c.getName()))
            .map(Cookie::getValue)
            .filter(v -> v != null && !v.isBlank())
            .findFirst();
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(authorizationRequest);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar OAuth2AuthorizationRequest", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(value);
            return objectMapper.readValue(new String(json, StandardCharsets.UTF_8), OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            // Cookie ausente/corrompido/adulterado — trata como ausente (refaz o fluxo).
            return null;
        }
    }
}
