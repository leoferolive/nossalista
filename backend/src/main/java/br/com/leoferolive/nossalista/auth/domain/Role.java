package br.com.leoferolive.nossalista.auth.domain;

/**
 * Papéis (roles) de usuários no sistema
 */
public enum Role {
    /**
     * Usuário regular com permissões padrão
     */
    USER,

    /**
     * Administrador com permissões elevadas
     */
    ADMIN
}
