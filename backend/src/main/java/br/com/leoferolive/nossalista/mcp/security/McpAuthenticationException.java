package br.com.leoferolive.nossalista.mcp.security;

/**
 * Lançada quando uma tool MCP é invocada sem um usuário autenticado resolvido
 * a partir do {@code SecurityContext}. Na prática não deveria ocorrer — o
 * matcher {@code /mcp/**} em {@code SecurityConfig} já exige autenticação
 * antes de qualquer tool ser despachada — mas serve como rede de segurança
 * caso o transporte MCP invoque uma tool fora do ciclo de vida esperado da
 * requisição HTTP.
 */
public class McpAuthenticationException extends RuntimeException {

    public McpAuthenticationException(String message) {
        super(message);
    }
}
