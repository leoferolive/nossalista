package br.com.leoferolive.nossalista.auth.provider;

import br.com.leoferolive.nossalista.auth.service.GoogleIdentityClaims;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Translates Google's OAuth2 user representation into the application's typed identity claims.
 *
 * <pre>{@code
 * GoogleIdentityClaims claims = adapter.from(googleUser);
 * }</pre>
 */
@Component
public class GoogleOAuth2ClaimsAdapter {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    /**
     * Extracts the Google attributes required by the identity resolver.
     *
     * <pre>{@code
     * GoogleIdentityClaims claims = adapter.from(googleUser);
     * }</pre>
     *
     * @param googleUser authenticated user supplied by Spring Security's Google client
     * @return typed claims for {@code GoogleIdentityService}
     */
    public GoogleIdentityClaims from(OAuth2User googleUser) {
        Map<String, Object> attributes = googleUser.getAttributes();
        return new GoogleIdentityClaims(
            GOOGLE_ISSUER,
            stringAttribute(attributes, "sub"),
            stringAttribute(attributes, "email"),
            booleanAttribute(attributes.get("email_verified")),
            stringAttribute(attributes, "name"),
            stringAttribute(attributes, "picture")
        );
    }

    private String stringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? null : value.toString();
    }

    private boolean booleanAttribute(Object value) {
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }
}
