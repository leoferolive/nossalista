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

import static org.assertj.core.api.Assertions.assertThat;

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
}
