package br.com.leoferolive.nossalista.mcp.security;

/**
 * Lançada quando um Personal Access Token de escopo {@code READ} tenta
 * chamar uma tool MCP de mutação. A restrição HTTP de {@code SecurityConfig}
 * (métodos seguros para PATs READ) não cobre o servidor MCP, porque todo o
 * protocolo trafega via {@code POST /mcp} — o enforcement de escopo precisa
 * acontecer na tool, via {@link McpSecurityContext#requireWriteAccess()}.
 */
public class McpScopeException extends RuntimeException {

    public McpScopeException(String message) {
        super(message);
    }
}
