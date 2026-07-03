package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

/**
 * Predicados compartilhados para as regras de autorização de Personal Access
 * Tokens (PAT) em {@link SecurityConfig}.
 *
 * <p>Uma requisição autenticada via PAT recebe, além das authorities
 * {@code ROLE_*} normais, a authority marcadora {@link #PAT_AUTHORITY} e a
 * authority de escopo ({@link TokenScope#authority()}). Essas duas
 * authorities extras permitem diferenciar, no {@code AuthorizationManager},
 * uma sessão JWT normal de uma sessão via PAT — sem alterar em nada o
 * comportamento do {@link JwtAuthenticationFilter}.</p>
 */
public final class PatAuthorizationSupport {

    /**
     * Authority marcadora: presente em autenticações originadas de um PAT OU de
     * um access token OAuth do servidor MCP ({@code McpOAuthTokenAuthenticationFilter},
     * Fase C — ver docs/DECISIONS.md D-021). Ambos são credenciais não-sessão com
     * escopo explícito ({@link TokenScope}), então compartilham esta authority
     * para que o enforcement de escopo em {@code McpSecurityContext.requireWriteAccess()}
     * funcione igual para os dois sem precisar conhecer a origem do token.
     */
    public static final String PAT_AUTHORITY = "PAT_AUTH";

    /**
     * Authority marcadora exclusiva de access tokens OAuth do servidor MCP
     * (distinta de {@link #PAT_AUTHORITY}, que os tokens OAuth TAMBÉM carregam).
     * Usada para bloquear esses tokens em {@code /api/**} — diferente de um PAT,
     * um token OAuth do MCP nunca deve acessar a API REST do SPA, nem em
     * métodos seguros.
     */
    public static final String MCP_OAUTH_AUTHORITY = "MCP_OAUTH_AUTH";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private PatAuthorizationSupport() {
    }

    /**
     * @param authentication autenticação corrente (pode ser {@code null})
     * @return true se há um principal autenticado e não-anônimo
     */
    public static boolean isAuthenticatedUser(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * @param authentication autenticação corrente (pode ser {@code null})
     * @return true se a autenticação foi originada de um Personal Access Token
     */
    public static boolean isPersonalAccessToken(Authentication authentication) {
        return hasAuthority(authentication, PAT_AUTHORITY);
    }

    /**
     * @param authentication autenticação corrente (pode ser {@code null})
     * @return true se a autenticação é um PAT com escopo {@link TokenScope#READ}
     */
    public static boolean hasReadOnlyScope(Authentication authentication) {
        return hasAuthority(authentication, TokenScope.READ.authority());
    }

    /**
     * @param authentication autenticação corrente (pode ser {@code null})
     * @return true se a autenticação foi originada de um access token OAuth do servidor MCP
     */
    public static boolean isMcpOAuthToken(Authentication authentication) {
        return hasAuthority(authentication, MCP_OAUTH_AUTHORITY);
    }

    /**
     * @param httpMethod método HTTP da requisição (ex.: {@code "GET"})
     * @return true se o método é considerado seguro/somente-leitura
     */
    public static boolean isSafeMethod(String httpMethod) {
        return httpMethod != null && SAFE_METHODS.contains(httpMethod.toUpperCase(java.util.Locale.ROOT));
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
