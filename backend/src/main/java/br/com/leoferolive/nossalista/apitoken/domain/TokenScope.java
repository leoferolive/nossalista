package br.com.leoferolive.nossalista.apitoken.domain;

/**
 * Escopo de acesso de um Personal Access Token (PAT).
 *
 * <p>{@link #READ} permite apenas métodos HTTP seguros (GET/HEAD/OPTIONS) em
 * {@code /api/**}; {@link #READ_WRITE} permite também métodos de mutação. Em
 * ambos os casos, o PAT nunca tem acesso a {@code /api/auth/**} nem a
 * {@code /api/users/me/tokens/**} — gestão de tokens exige sessão JWT normal.</p>
 */
public enum TokenScope {

    READ("SCOPE_READ"),
    READ_WRITE("SCOPE_READ_WRITE");

    private final String authority;

    TokenScope(String authority) {
        this.authority = authority;
    }

    /**
     * Nome da {@code GrantedAuthority} Spring Security correspondente a este escopo.
     *
     * @return authority no formato {@code SCOPE_*}
     */
    public String authority() {
        return authority;
    }
}
