package br.com.leoferolive.nossalista.support;

import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Simula a sessão web do profile de teste. O JWT fica no cookie HttpOnly de
 * sessão; o segundo cookie/header representa o double-submit CSRF da SPA.
 */
public final class SessionCookieRequestPostProcessor {

    private static final String SESSION_COOKIE_NAME = "nl_session";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String CSRF_TOKEN = "test-csrf-token";

    private SessionCookieRequestPostProcessor() {
    }

    public static RequestPostProcessor session(String token) {
        String rawToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        return request -> {
            MockHttpServletRequest mockRequest = (MockHttpServletRequest) request;
            mockRequest.setCookies(
                new Cookie(SESSION_COOKIE_NAME, rawToken),
                new Cookie(CSRF_COOKIE_NAME, CSRF_TOKEN)
            );
            mockRequest.addHeader(CSRF_HEADER_NAME, CSRF_TOKEN);
            return mockRequest;
        };
    }
}
