package br.com.leoferolive.nossalista.user.domain;

/**
 * Tipos de provedores de autenticação suportados pela aplicação
 */
public enum AuthProvider {
    /**
     * Autenticação com email e senha
     */
    EMAIL,

    /**
     * Autenticação via Google OAuth2
     */
    GOOGLE
}
