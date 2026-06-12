package br.com.leoferolive.nossalista.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
 * A desserialização é restringida por {@link ObjectInputFilter} às classes do
 * próprio Spring/JDK, mitigando ataques de desserialização via cookie adulterado.</p>
 */
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "OAUTH2_AUTH_REQUEST";
    private static final int TTL_SECONDS = 180;

    /** Whitelist de classes permitidas na desserialização do cookie. */
    private static final ObjectInputFilter SAFE_FILTER = ObjectInputFilter.Config.createFilter(
        "org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;"
            + "org.springframework.security.oauth2.core.*;"
            + "org.springframework.security.oauth2.core.endpoint.*;"
            + "java.util.*;java.lang.*;java.net.*;!*");

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
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(authorizationRequest);
            oos.flush();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao serializar OAuth2AuthorizationRequest", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                ois.setObjectInputFilter(SAFE_FILTER);
                Object obj = ois.readObject();
                return (obj instanceof OAuth2AuthorizationRequest req) ? req : null;
            }
        } catch (InvalidClassException e) {
            // Classe fora da whitelist (cookie adulterado) — trata como ausente.
            return null;
        } catch (Exception e) {
            // Cookie corrompido/incompatível — trata como ausente (refaz o fluxo).
            return null;
        }
    }
}
