package br.com.leoferolive.nossalista.mcp.security;

/**
 * Lançada quando um usuário excede o teto de mutações por minuto nas tools
 * MCP (ver {@code McpMutationRateLimiter#enforce()}). Vira um resultado de
 * tool com {@code isError: true} pelo mesmo mecanismo genérico do SDK que
 * converte {@link McpAuthenticationException}/{@link McpScopeException} —
 * nunca stack trace nem erro de protocolo.
 */
public class McpRateLimitExceededException extends RuntimeException {

    public McpRateLimitExceededException(String message) {
        super(message);
    }
}
