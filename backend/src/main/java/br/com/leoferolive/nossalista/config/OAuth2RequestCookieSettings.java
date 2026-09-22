package br.com.leoferolive.nossalista.config;

import org.springframework.core.env.Environment;

import java.util.Arrays;

/** Resolves the profile-specific attributes of the OAuth authorization-request cookie. */
record OAuth2RequestCookieSettings(String name, boolean secure) {

    static OAuth2RequestCookieSettings resolve(Environment environment) {
        boolean production = hasProductionProfile(environment);
        String name = configuredCookieName(environment, production);
        boolean secure = configuredCookieSecure(environment, production);
        validateConfiguration(production, name, secure);
        return new OAuth2RequestCookieSettings(name, secure);
    }

    private static boolean hasProductionProfile(Environment environment) {
        return environment != null && Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private static String configuredCookieName(Environment environment, boolean production) {
        if (environment == null) {
            return CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME;
        }
        String configuredName = environment.getProperty("app.auth.oauth2-request-cookie.name");
        return configuredName == null ? defaultCookieName(environment, production) : configuredName;
    }

    private static String defaultCookieName(Environment environment, boolean production) {
        String sessionName = environment.getProperty("app.auth.session-cookie.name",
            production ? CookieOAuth2AuthorizationRequestRepository.PRODUCTION_COOKIE_NAME
                : CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        return sessionName.startsWith("__Host-")
            ? CookieOAuth2AuthorizationRequestRepository.PRODUCTION_COOKIE_NAME
            : CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME;
    }

    private static boolean configuredCookieSecure(Environment environment, boolean production) {
        return environment != null && Boolean.parseBoolean(environment.getProperty(
            "app.auth.oauth2-request-cookie.secure", Boolean.toString(production)));
    }

    private static void validateConfiguration(boolean production, String name, boolean secure) {
        validateHostPrefix(name, secure);
        if (production) {
            validateProductionCookie(name, secure);
            return;
        }
        validateNonProductionCookie(name, secure);
    }

    private static void validateHostPrefix(String name, boolean secure) {
        if (name.startsWith("__Host-") && !secure) {
            throw new IllegalStateException("Cookies com prefixo __Host- exigem Secure=true");
        }
    }

    private static void validateProductionCookie(String name, boolean secure) {
        if (!CookieOAuth2AuthorizationRequestRepository.PRODUCTION_COOKIE_NAME.equals(name) || !secure) {
            throw new IllegalStateException(
                "O profile prod exige cookie OAuth2 __Host-nl_oauth2_request com Secure=true");
        }
    }

    private static void validateNonProductionCookie(String name, boolean secure) {
        if (CookieOAuth2AuthorizationRequestRepository.PRODUCTION_COOKIE_NAME.equals(name) || secure) {
            throw new IllegalStateException(
                "Profiles dev/test exigem cookie OAuth2 nl_oauth2_request com Secure=false");
        }
    }
}
