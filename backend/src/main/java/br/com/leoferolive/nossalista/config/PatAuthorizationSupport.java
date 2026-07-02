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

    /** Authority marcadora: presente somente em autenticações originadas de um PAT. */
    public static final String PAT_AUTHORITY = "PAT_AUTH";

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
