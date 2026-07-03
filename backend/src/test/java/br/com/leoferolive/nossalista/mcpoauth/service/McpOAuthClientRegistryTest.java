package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties.ClientDefinition;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRegisteredClient;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthInvalidRedirectUriException;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthUnknownClientException;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthRegisteredClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpOAuthClientRegistryTest {

    private McpOAuthProperties properties;
    private McpOAuthRegisteredClientRepository dynamicClientRepository;
    private McpOAuthClientRegistry registry;

    @BeforeEach
    void setUp() {
        properties = new McpOAuthProperties();

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
        // Sem clientes dinâmicos registrados neste teste — todo lookup por DCR
        // cai no fallback padrão do Mockito (Optional.empty()).
        dynamicClientRepository = mock(McpOAuthRegisteredClientRepository.class);
        registry = new McpOAuthClientRegistry(properties, dynamicClientRepository);
    }

    private McpOAuthRegisteredClient dynamicClient(String clientId, String name, List<String> redirectUris) {
        McpOAuthRegisteredClient client = new McpOAuthRegisteredClient();
        client.setClientId(clientId);
        client.setClientName(name);
        client.setRedirectUris(redirectUris);
        client.setScope("read read_write");
        client.setTokenEndpointAuthMethod("none");
        client.setGrantTypes(List.of("authorization_code", "refresh_token"));
        return client;
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
    void validateRedirectUriAcceptsIpv6LoopbackForLoopbackClient() {
        // RFC 8252 §7.3 não distingue IPv4/IPv6 para a exceção de loopback —
        // achado NIT do QA (Fase C.1). java.net.URI#getHost() preserva os
        // colchetes do literal IPv6 ("[::1]"), daí o formato exato aqui.
        ClientDefinition client = registry.require("claude-code");
        registry.validateRedirectUri(client, "http://[::1]:12345/callback");
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

    @Test
    void validateRedirectUriRejectsNullRedirectUri() {
        ClientDefinition client = registry.require("claude-ai");
        assertThatThrownBy(() -> registry.validateRedirectUri(client, null))
            .isInstanceOf(OAuthInvalidRedirectUriException.class);
    }

    @Test
    void validateRedirectUriRejectsBlankRedirectUri() {
        ClientDefinition client = registry.require("claude-ai");
        assertThatThrownBy(() -> registry.validateRedirectUri(client, "   "))
            .isInstanceOf(OAuthInvalidRedirectUriException.class);
    }

    @Test
    void validateRedirectUriRejectsLoopbackUriWithoutHostForLoopbackClient() {
        // "http:///callback" tem scheme=http e path=/callback, mas host nulo (sem
        // autoridade) — não deve satisfazer a regra de loopback mesmo com path certo.
        ClientDefinition client = registry.require("claude-code");
        assertThatThrownBy(() -> registry.validateRedirectUri(client, "http:///callback"))
            .isInstanceOf(OAuthInvalidRedirectUriException.class);
    }

    @Test
    void findReturnsEmptyForNullClientId() {
        assertThat(registry.find(null)).isEmpty();
    }

    // -------------------------------------------------- clientes dinâmicos (DCR, Fase C.1/D-024)

    @Test
    void requireResolvesDynamicClientWhenNoStaticMatch() {
        McpOAuthRegisteredClient dynamic = dynamicClient(
            "dcr_abc123", "DCR Test Client", List.of("https://app.example.com/callback"));
        when(dynamicClientRepository.findByClientId("dcr_abc123")).thenReturn(Optional.of(dynamic));

        ClientDefinition resolved = registry.require("dcr_abc123");

        assertThat(resolved.getId()).isEqualTo("dcr_abc123");
        assertThat(resolved.getName()).isEqualTo("DCR Test Client");
        assertThat(resolved.getRedirectUris()).containsExactly("https://app.example.com/callback");
        assertThat(resolved.isAllowLoopbackRedirect()).isFalse();
    }

    @Test
    void requireNeverTouchesLastUsedAtForDynamicClient() {
        // Achado do review (I-1, DoS que anulava o TTL): require() é chamado por
        // GET /oauth/authorize, PÚBLICO e sem prova de posse nenhuma — marcar
        // "usado" ali permitia imunizar um cliente-zumbi contra o cleanup com uma
        // única chamada anônima. touchLastUsedAt só acontece via
        // McpOAuthClientRegistry#touchIfDynamic, chamado na troca code->token
        // (ver McpOAuthTokenService#issueTokenPair).
        McpOAuthRegisteredClient dynamic = dynamicClient(
            "dcr_touch", "Touch Test", List.of("https://app.example.com/callback"));
        when(dynamicClientRepository.findByClientId("dcr_touch")).thenReturn(Optional.of(dynamic));

        registry.require("dcr_touch");

        verify(dynamicClientRepository, times(0)).touchLastUsedAt(any(), any());
    }

    @Test
    void requireDoesNotTouchDynamicRepositoryForStaticClient() {
        registry.require("claude-ai");

        verify(dynamicClientRepository, times(0)).touchLastUsedAt(any(), any());
    }

    @Test
    void touchIfDynamicMarksLastUsedAtForDynamicClient() {
        registry.touchIfDynamic("dcr_touch");

        verify(dynamicClientRepository, times(1)).touchLastUsedAt(eq("dcr_touch"), any(LocalDateTime.class));
    }

    @Test
    void touchIfDynamicIsNoOpForStaticClient() {
        registry.touchIfDynamic("claude-ai");

        verify(dynamicClientRepository, times(0)).touchLastUsedAt(any(), any());
    }

    @Test
    void touchIfDynamicIsNoOpForNullClientId() {
        registry.touchIfDynamic(null);

        verify(dynamicClientRepository, times(0)).touchLastUsedAt(any(), any());
    }

    @Test
    void requireThrowsUnknownClientWhenNeitherStaticNorDynamicMatches() {
        when(dynamicClientRepository.findByClientId("does-not-exist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registry.require("does-not-exist"))
            .isInstanceOf(OAuthUnknownClientException.class);
    }

    @Test
    void findResolvesDynamicClientWithoutTouchingLastUsedAt() {
        McpOAuthRegisteredClient dynamic = dynamicClient(
            "dcr_view", "View Test", List.of("https://app.example.com/callback"));
        when(dynamicClientRepository.findByClientId("dcr_view")).thenReturn(Optional.of(dynamic));

        Optional<ClientDefinition> resolved = registry.find("dcr_view");

        assertThat(resolved).isPresent();
        verify(dynamicClientRepository, times(0)).touchLastUsedAt(any(), any());
    }

    @Test
    void dynamicClientFallsBackToClientIdWhenClientNameIsBlank() {
        McpOAuthRegisteredClient dynamic = dynamicClient("dcr_noname", null, List.of("https://app.example.com/callback"));
        when(dynamicClientRepository.findByClientId("dcr_noname")).thenReturn(Optional.of(dynamic));

        ClientDefinition resolved = registry.require("dcr_noname");

        assertThat(resolved.getName()).isEqualTo("dcr_noname");
    }

    @Test
    void validateRedirectUriForDynamicClientIsExactMatchOnly() {
        McpOAuthRegisteredClient dynamic = dynamicClient(
            "dcr_exact", "Exact Test", List.of("http://localhost:54321/callback"));
        when(dynamicClientRepository.findByClientId("dcr_exact")).thenReturn(Optional.of(dynamic));
        ClientDefinition client = registry.require("dcr_exact");

        registry.validateRedirectUri(client, "http://localhost:54321/callback");
        // Diferente do cliente estático loopback (claude-code): sem exceção de porta
        // variável para clientes dinâmicos — o hardening do registro já exige porta
        // explícita, então outra porta é rejeitada.
        assertThatThrownBy(() -> registry.validateRedirectUri(client, "http://localhost:9999/callback"))
            .isInstanceOf(OAuthInvalidRedirectUriException.class);
    }
}
