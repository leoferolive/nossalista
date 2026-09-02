package br.com.leoferolive.nossalista.auth.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Centraliza a emissão e leitura do cookie que transporta o JWT de sessão.
 *
 * <p>Em produção o nome {@code __Host-nl_session} exige {@code Secure},
 * {@code Path=/} e ausência de {@code Domain}; essas propriedades evitam que
 * outro host do domínio injete ou sobrescreva a sessão.</p>
 */
@Service
public class SessionCookieService {

    private final Environment environment;

    @Value("${app.auth.session-cookie.name}")
    private String cookieName;

    @Value("${app.auth.session-cookie.secure}")
    private boolean secure;

    @Value("${app.auth.session-cookie.same-site:Lax}")
    private String sameSite;

    @Value("${jwt.expiration}")
    private long expirationMillis;

    public SessionCookieService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateConfiguration() {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        validateHostPrefix();
        validateProductionConfiguration(production);
        validateExpiration();
    }

    private void validateHostPrefix() {
        if (cookieName.startsWith("__Host-") && !secure) {
            throw new IllegalStateException("Cookies com prefixo __Host- exigem Secure=true");
        }
    }

    private void validateProductionConfiguration(boolean production) {
        boolean hasSecureHostCookie = cookieName.startsWith("__Host-") && secure;
        boolean sameSiteLax = "Lax".equalsIgnoreCase(sameSite);

        if (production && (!hasSecureHostCookie || !sameSiteLax)) {
            throw new IllegalStateException(
                "O profile prod exige cookie de sessão __Host- com Secure=true e SameSite=Lax");
        }
    }

    private void validateExpiration() {
        if (expirationMillis <= 0) {
            throw new IllegalStateException("jwt.expiration deve ser positivo para o cookie de sessão");
        }
    }

    public void writeSession(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE,
            buildCookie(token, Duration.ofMillis(expirationMillis)).toString());
    }

    public void clearSession(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
    }

    public Optional<String> extractToken(HttpServletRequest request) {
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

    public boolean hasSessionCookie(HttpServletRequest request) {
        return extractToken(request).isPresent();
    }

    public boolean isSecure() {
        return secure;
    }

    public String getCookieName() {
        return cookieName;
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value)
            .path("/")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .maxAge(maxAge)
            .build();
    }
}
