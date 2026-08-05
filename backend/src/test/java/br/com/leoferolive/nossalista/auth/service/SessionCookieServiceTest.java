package br.com.leoferolive.nossalista.auth.service;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionCookieServiceTest {

    @Test
    void shouldAcceptDevelopmentConfigurationAndWriteAndClearSessionCookie() {
        SessionCookieService service = service(new MockEnvironment(), "nl_session", false, "Lax", 604_800_000L);
        service.validateConfiguration();

        MockHttpServletResponse writeResponse = new MockHttpServletResponse();
        service.writeSession(writeResponse, "signed-session");

        assertThat(writeResponse.getHeader("Set-Cookie"))
            .contains("nl_session=signed-session", "Path=/", "HttpOnly", "SameSite=Lax", "Max-Age=604800")
            .doesNotContain("Secure");
        assertThat(service.isSecure()).isFalse();
        assertThat(service.getCookieName()).isEqualTo("nl_session");

        MockHttpServletResponse clearResponse = new MockHttpServletResponse();
        service.clearSession(clearResponse);

        assertThat(clearResponse.getHeader("Set-Cookie"))
            .contains("nl_session=", "Max-Age=0", "HttpOnly", "SameSite=Lax");
    }

    @Test
    void shouldReadOnlyNonBlankCookieWithConfiguredName() {
        SessionCookieService service = service(new MockEnvironment(), "nl_session", false, "Lax", 1L);

        MockHttpServletRequest noCookies = new MockHttpServletRequest();
        assertThat(service.extractToken(noCookies)).isEmpty();
        assertThat(service.hasSessionCookie(noCookies)).isFalse();

        MockHttpServletRequest ignoredCookies = new MockHttpServletRequest();
        ignoredCookies.setCookies(new Cookie("other", "value"), new Cookie("nl_session", " "));
        assertThat(service.extractToken(ignoredCookies)).isEmpty();

        MockHttpServletRequest sessionCookie = new MockHttpServletRequest();
        sessionCookie.setCookies(new Cookie("other", "value"), new Cookie("nl_session", "signed-session"));
        assertThat(service.extractToken(sessionCookie)).contains("signed-session");
        assertThat(service.hasSessionCookie(sessionCookie)).isTrue();
    }

    @Test
    void shouldAcceptSecureHostCookieOnlyWithProductionRequirements() {
        SessionCookieService service = service(production(),
            "__Host-nl_session", true, "lAx", 1L);

        service.validateConfiguration();
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.writeSession(response, "signed-session");

        assertThat(response.getHeader("Set-Cookie"))
            .contains("__Host-nl_session=signed-session", "Path=/", "Secure", "HttpOnly", "SameSite=lAx");
        assertThat(service.isSecure()).isTrue();
        assertThat(service.getCookieName()).isEqualTo("__Host-nl_session");
    }

    @Test
    void shouldRejectUnsafeHostPrefixAndInvalidProductionConfiguration() {
        assertThatThrownBy(() -> service(new MockEnvironment(), "__Host-nl_session", false, "Lax", 1L)
            .validateConfiguration())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("__Host-");

        assertThatThrownBy(() -> service(production(), "nl_session", true, "Lax", 1L)
            .validateConfiguration())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("profile prod");

        assertThatThrownBy(() -> service(production(), "__Host-nl_session", true, "Strict", 1L)
            .validateConfiguration())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SameSite=Lax");
    }

    @Test
    void shouldRejectNonPositiveExpiration() {
        assertThatThrownBy(() -> service(new MockEnvironment(), "nl_session", false, "Lax", 0L)
            .validateConfiguration())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jwt.expiration");
    }

    private MockEnvironment production() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }

    private SessionCookieService service(MockEnvironment environment, String name, boolean secure, String sameSite,
                                         long expiration) {
        SessionCookieService service = new SessionCookieService(environment);
        ReflectionTestUtils.setField(service, "cookieName", name);
        ReflectionTestUtils.setField(service, "secure", secure);
        ReflectionTestUtils.setField(service, "sameSite", sameSite);
        ReflectionTestUtils.setField(service, "expirationMillis", expiration);
        return service;
    }
}
