package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties.ClientDefinition;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthInvalidRedirectUriException;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthUnknownClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpOAuthClientRegistryTest {

    private McpOAuthClientRegistry registry;

    @BeforeEach
    void setUp() {
        McpOAuthProperties properties = new McpOAuthProperties();

        ClientDefinition exact = new ClientDefinition();
        exact.setId("claude-ai");
        exact.setName("Claude (claude.ai)");
        exact.setRedirectUris(List.of("https://claude.ai/api/mcp/auth_callback"));
        exact.setAllowLoopbackRedirect(false);

        ClientDefinition loopback = new ClientDefinition();
        loopback.setId("claude-code");
        loopback.setName("Claude Code");
        loopback.setRedirectUris(List.of());
        loopback.setAllowLoopbackRedirect(true);

        properties.setClients(List.of(exact, loopback));
        registry = new McpOAuthClientRegistry(properties);
    }

    @Test
    void requireReturnsRegisteredClient() {
        assertThat(registry.require("claude-ai").getId()).isEqualTo("claude-ai");
    }

    @Test
    void requireThrowsForUnknownClient() {
        assertThatThrownBy(() -> registry.require("unknown"))
            .isInstanceOf(OAuthUnknownClientException.class);
    }

    @Test
    void validateRedirectUriAcceptsExactMatch() {
        ClientDefinition client = registry.require("claude-ai");
        registry.validateRedirectUri(client, "https://claude.ai/api/mcp/auth_callback");
        // não lança -> passou
    }

    @Test
    void validateRedirectUriRejectsUnregisteredUri() {
        ClientDefinition client = registry.require("claude-ai");
        assertThatThrownBy(() -> registry.validateRedirectUri(client, "https://evil.example.com/callback"))
            .isInstanceOf(OAuthInvalidRedirectUriException.class);
    }

    @Test
    void validateRedirectUriRejectsHttpsClaudeAiOverridingToHttp() {
        ClientDefinition client = registry.require("claude-ai");
        assertThatThrownBy(() -> registry.validateRedirectUri(client, "http://claude.ai/api/mcp/auth_callback"))
            .isInstanceOf(OAuthInvalidRedirectUriException.class);
    }

    @Test
    void validateRedirectUriAcceptsLoopbackWithAnyPortForLoopbackClient() {
        ClientDefinition client = registry.require("claude-code");
        registry.validateRedirectUri(client, "http://localhost:12345/callback");
        registry.validateRedirectUri(client, "http://127.0.0.1:9999/callback");
    }

    @Test
    void validateRedirectUriRejectsLoopbackWithWrongPathForLoopbackClient() {
        ClientDefinition client = registry.require("claude-code");
        assertThatThrownBy(() -> registry.validateRedirectUri(client, "http://localhost:12345/other-path"))
            .isInstanceOf(OAuthInvalidRedirectUriException.class);
    }

    @Test
    void validateRedirectUriRejectsNonLoopbackHostForLoopbackClient() {
        ClientDefinition client = registry.require("claude-code");
        assertThatThrownBy(() -> registry.validateRedirectUri(client, "http://evil.example.com:12345/callback"))
            .isInstanceOf(OAuthInvalidRedirectUriException.class);
    }

    @Test
    void validateRedirectUriRejectsHttpsSchemeForLoopbackRule() {
        // A regra de loopback (RFC 8252 §7.3) é sempre http, nunca https.
        ClientDefinition client = registry.require("claude-code");
        assertThatThrownBy(() -> registry.validateRedirectUri(client, "https://localhost:12345/callback"))
            .isInstanceOf(OAuthInvalidRedirectUriException.class);
    }
}
