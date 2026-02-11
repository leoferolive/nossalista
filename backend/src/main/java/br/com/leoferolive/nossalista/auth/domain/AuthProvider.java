package br.com.leoferolive.nossalista.auth.domain;

/**
 * Authentication provider types supported by the application
 */
public enum AuthProvider {
    /**
     * Email and password authentication
     */
    EMAIL,

    /**
     * Google OAuth2 authentication
     */
    GOOGLE
}
