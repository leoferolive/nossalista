package br.com.leoferolive.nossalista.mcpoauth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que o filtro do access token OAuth do MCP re-executa no dispatch ASYNC
 * (fix de D-026) — o caminho de auth mais critico para o {@code /mcp}. Ver
 * {@code AuthFilterAsyncDispatchTest} (config) e {@code AsyncDispatchReauthenticationTest}
 * para o contexto completo do bug.
 */
class McpOAuthTokenAuthenticationFilterAsyncTest {

    @Test
    @DisplayName("McpOAuthTokenAuthenticationFilter re-autentica no dispatch async")
    void reauthenticatesOnAsyncDispatch() {
        assertThat(new McpOAuthTokenAuthenticationFilter(null, null).shouldNotFilterAsyncDispatch()).isFalse();
    }
}
