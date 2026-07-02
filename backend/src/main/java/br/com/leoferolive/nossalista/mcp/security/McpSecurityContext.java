package br.com.leoferolive.nossalista.mcp.security;

import br.com.leoferolive.nossalista.config.PatAuthorizationSupport;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolve a identidade do usuário autenticado e aplica o enforcement de
 * escopo de Personal Access Token para as tools MCP, lendo sempre do
 * {@link SecurityContextHolder} corrente.
 *
 * <p>Este componente não guarda nenhum estado de usuário entre chamadas: a
 * cada invocação de tool, {@link #currentUser()} resolve de novo a partir da
 * autenticação da requisição HTTP corrente (populada pelos filtros
 * {@code JwtAuthenticationFilter}/{@code PersonalAccessTokenAuthenticationFilter}
 * da Fase A, que rodam para toda requisição, inclusive {@code /mcp}). Isso
 * garante isolamento entre usuários mesmo que duas conexões MCP concorrentes
 * cheguem em threads diferentes do mesmo processo.</p>
 */
@Component
public class McpSecurityContext {

    /**
     * @return o usuário autenticado da requisição corrente
     * @throws McpAuthenticationException se não houver autenticação válida no contexto
     */
    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!PatAuthorizationSupport.isAuthenticatedUser(authentication)
            || !(authentication.getPrincipal() instanceof User user)) {
            throw new McpAuthenticationException(
                "Not authenticated. Connect with a valid Bearer JWT or Personal Access Token (nlmcp_...).");
        }
        return user;
    }

    /**
     * Bloqueia a chamada corrente se ela vier de um Personal Access Token de
     * escopo {@code READ}. Deve ser chamado no início de toda tool que muta
     * dados (criar, renomear, excluir, adicionar/editar/remover itens,
     * compartilhar, remover membro).
     *
     * @throws McpScopeException se a autenticação corrente for um PAT READ-only
     */
    public void requireWriteAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (PatAuthorizationSupport.isPersonalAccessToken(authentication)
            && PatAuthorizationSupport.hasReadOnlyScope(authentication)) {
            throw new McpScopeException(
                "Your Personal Access Token has read-only scope and cannot call tools that modify "
                    + "data. Ask the user to create a new token with READ_WRITE scope to perform "
                    + "this action.");
        }
    }
}
