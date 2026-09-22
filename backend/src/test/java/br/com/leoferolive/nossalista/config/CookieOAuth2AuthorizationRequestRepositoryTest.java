package br.com.leoferolive.nossalista.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;
import java.util.Set;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Garante que o authorization-request (state OAuth2) faz round-trip por cookie
 * — a correção para o login Google sob STATELESS (sem HttpSession).
 */
@DisplayName("CookieOAuth2AuthorizationRequestRepository")
class CookieOAuth2AuthorizationRequestRepositoryTest {

    private final CookieOAuth2AuthorizationRequestRepository repo =
        new CookieOAuth2AuthorizationRequestRepository();

    private OAuth2AuthorizationRequest sample(String state) {
        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("client-123")
            .redirectUri("https://nossalista.leoferolive.com.br/api/auth/google/callback")
            .scopes(Set.of("openid", "email", "profile"))
            .state(state)
            .additionalParameters(Map.of("nonce", "abc123"))
            .build();
    }

    private String cookieValueFrom(MockHttpServletResponse response) {
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull().contains(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME + "=");
        int start = setCookie.indexOf('=') + 1;
        int end = setCookie.indexOf(';', start);
        return setCookie.substring(start, end < 0 ? setCookie.length() : end);
    }

    @Test
    @DisplayName("save -> cookie -> load preserva o state e os campos do fluxo")
    void roundTripPreservesAuthorizationRequest() {
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        repo.saveAuthorizationRequest(sample("state-xyz-987"), new MockHttpServletRequest(), saveResponse);

        String cookieValue = cookieValueFrom(saveResponse);
        assertThat(cookieValue).isNotBlank();
        assertThat(saveResponse.getHeader(HttpHeaders.SET_COOKIE))
            .contains("Path=/", "HttpOnly", "SameSite=Lax", "Max-Age=180")
            .doesNotContain("Secure");
        assertThat(cookieValue).contains(".");

        MockHttpServletRequest loadRequest = new MockHttpServletRequest();
        loadRequest.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, cookieValue));

        OAuth2AuthorizationRequest loaded = repo.loadAuthorizationRequest(loadRequest);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getState()).isEqualTo("state-xyz-987");
        assertThat(loaded.getClientId()).isEqualTo("client-123");
        assertThat(loaded.getAuthorizationUri()).isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(loaded.getScopes()).containsExactlyInAnyOrder("openid", "email", "profile");
        assertThat(loaded.getAdditionalParameters()).containsEntry("nonce", "abc123");
    }

    @Test
    @DisplayName("remove devolve o request e zera o cookie")
    void removeReturnsAndClearsCookie() {
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        repo.saveAuthorizationRequest(sample("state-to-remove"), new MockHttpServletRequest(), saveResponse);
        String cookieValue = cookieValueFrom(saveResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, cookieValue));
        MockHttpServletResponse removeResponse = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed = repo.removeAuthorizationRequest(request, removeResponse);

        assertThat(removed).isNotNull();
        assertThat(removed.getState()).isEqualTo("state-to-remove");
        // cookie de limpeza (Max-Age=0)
        assertThat(removeResponse.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    @DisplayName("cookie ausente, vazio ou adulterado -> null (refaz o fluxo, sem quebrar)")
    void absentOrTamperedCookieYieldsNull() {
        assertThat(repo.loadAuthorizationRequest(new MockHttpServletRequest())).isNull();

        MockHttpServletRequest tampered = new MockHttpServletRequest();
        tampered.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, "not-valid-base64-$$$"));
        assertThat(repo.loadAuthorizationRequest(tampered)).isNull();
    }

    @Test
    @DisplayName("adulterar o payload ou a assinatura falha fechado")
    void tamperedEnvelopeFailsClosedAndRemoveClearsCookie() {
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        repo.saveAuthorizationRequest(sample("state-safe"), new MockHttpServletRequest(), saveResponse);
        String cookieValue = cookieValueFrom(saveResponse);
        String tampered = "A" + cookieValue.substring(1);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, tampered));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(repo.removeAuthorizationRequest(request, response)).isNull();
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    @DisplayName("profile prod usa cookie __Host- seguro")
    void productionCookieUsesHostPrefixAndSecure() {
        CookieOAuth2AuthorizationRequestRepository production = repository("prod", "", Clock.systemUTC());
        MockHttpServletResponse response = new MockHttpServletResponse();

        production.saveAuthorizationRequest(sample("prod-state"), new MockHttpServletRequest(), response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
            .contains("__Host-nl_oauth2_request=", "Path=/", "Secure", "HttpOnly", "SameSite=Lax")
            .doesNotContain("Domain=");
    }

    @Test
    @DisplayName("aceita chave anterior antes do prazo de aposentadoria de 180 segundos")
    void previousSigningKeyIsAcceptedBeforeRetirementDeadline() {
        Instant rotationStartedAt = Instant.parse("2026-09-19T00:00:00Z");
        Instant retirementDeadline = rotationStartedAt.plusSeconds(
            CookieOAuth2AuthorizationRequestRepository.TTL_SECONDS);
        String oldKey = "old-oauth2-request-signing-key-minimum-32-bytes";
        CookieOAuth2AuthorizationRequestRepository old = repositoryWithKey("", oldKey,
            Clock.fixed(rotationStartedAt, ZoneOffset.UTC));
        MockHttpServletResponse saved = new MockHttpServletResponse();
        old.saveAuthorizationRequest(sample("rotated-state"), new MockHttpServletRequest(), saved);

        String cookie = cookieValueFrom(saved);
        CookieOAuth2AuthorizationRequestRepository rotated = repositoryWithKey(oldKey,
            "new-oauth2-request-signing-key-minimum-32-bytes", retirementDeadline,
            Clock.fixed(retirementDeadline.minusSeconds(1), ZoneOffset.UTC));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, cookie));

        assertThat(rotated.loadAuthorizationRequest(request).getState()).isEqualTo("rotated-state");
    }

    @Test
    @DisplayName("rejeita envelope da chave anterior depois do prazo de aposentadoria")
    void previousSigningKeyIsRejectedAfterRetirementDeadline() {
        Instant rotationStartedAt = Instant.parse("2026-09-19T00:00:00Z");
        Instant retirementDeadline = rotationStartedAt.plusSeconds(
            CookieOAuth2AuthorizationRequestRepository.TTL_SECONDS);
        String oldKey = "old-oauth2-request-signing-key-minimum-32-bytes";
        Clock issuedBeforeDeadline = Clock.fixed(retirementDeadline.minusSeconds(1), ZoneOffset.UTC);
        CookieOAuth2AuthorizationRequestRepository old = repositoryWithKey("", oldKey,
            issuedBeforeDeadline);
        MockHttpServletResponse saved = new MockHttpServletResponse();
        old.saveAuthorizationRequest(sample("expired-rotation-state"), new MockHttpServletRequest(), saved);

        CookieOAuth2AuthorizationRequestRepository rotated = repositoryWithKey(oldKey,
            "new-oauth2-request-signing-key-minimum-32-bytes", retirementDeadline,
            Clock.fixed(retirementDeadline.plusSeconds(1), ZoneOffset.UTC));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME,
            cookieValueFrom(saved)));

        assertThat(rotated.loadAuthorizationRequest(request)).isNull();
    }

    @Test
    @DisplayName("rejeita chave ausente, curta ou igual ao JWT")
    void signingKeyConfigurationFailsClosed() {
        assertThatThrownBy(() -> repositoryWithKey("", "short", Clock.systemUTC()))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 bytes");
        String jwt = "jwt-secret-minimum-32-bytes-for-testing-purpose";
        assertThatThrownBy(() -> new CookieOAuth2AuthorizationRequestRepository(
            null, jwt, "", jwt, Clock.systemUTC()))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("distinta");
    }

    @Test
    @DisplayName("rejeita prazo de aposentadoria acima da janela de 180 segundos")
    void retirementDeadlineCannotExceedRotationWindow() {
        Instant rotationStartedAt = Instant.parse("2026-09-19T00:00:00Z");

        assertThatThrownBy(() -> repositoryWithKey(
            "old-oauth2-request-signing-key-minimum-32-bytes",
            "new-oauth2-request-signing-key-minimum-32-bytes",
            rotationStartedAt.plusSeconds(CookieOAuth2AuthorizationRequestRepository.TTL_SECONDS + 1),
            Clock.fixed(rotationStartedAt, ZoneOffset.UTC)))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("180 segundos");
    }

    private CookieOAuth2AuthorizationRequestRepository repository(String profile, String previous, Clock clock) {
        org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment();
        environment.setActiveProfiles(profile);
        return new CookieOAuth2AuthorizationRequestRepository(environment,
            "current-oauth2-request-signing-key-minimum-32-bytes", previous,
            "jwt-secret-minimum-32-bytes-for-testing-purpose", clock);
    }

    private CookieOAuth2AuthorizationRequestRepository repositoryWithKey(String previous, String current, Clock clock) {
        return repositoryWithKey(previous, current, null, clock);
    }

    private CookieOAuth2AuthorizationRequestRepository repositoryWithKey(
        String previous, String current, Instant retirementDeadline, Clock clock
    ) {
        org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment();
        if (retirementDeadline != null) {
            environment.setProperty("app.auth.oauth2-request-cookie.previous-signing-key-retirement-deadline",
                retirementDeadline.toString());
        }
        return new CookieOAuth2AuthorizationRequestRepository(environment, current, previous,
            "jwt-secret-minimum-32-bytes-for-testing-purpose", clock);
    }
}
