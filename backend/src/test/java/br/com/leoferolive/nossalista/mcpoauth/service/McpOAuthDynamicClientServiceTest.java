package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRegisteredClient;
import br.com.leoferolive.nossalista.mcpoauth.dto.ClientRegistrationRequest;
import br.com.leoferolive.nossalista.mcpoauth.dto.ClientRegistrationResponse;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthClientRegistrationException;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthRegisteredClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários de {@link McpOAuthDynamicClientService} — hardening de
 * {@code redirect_uris} (RFC 7591, Fase C.1/D-024), defaults de metadata, e o
 * teto global de clientes registrados.
 */
@ExtendWith(MockitoExtension.class)
class McpOAuthDynamicClientServiceTest {

    @Mock
    private McpOAuthRegisteredClientRepository repository;

    private McpOAuthDynamicClientService service;

    @BeforeEach
    void setUp() {
        McpOAuthProperties properties = new McpOAuthProperties();
        service = new McpOAuthDynamicClientService(repository, properties);
    }

    private ClientRegistrationRequest requestWithRedirectUris(List<String> redirectUris) {
        return new ClientRegistrationRequest(redirectUris, null, null, null, null, "Test Client", null);
    }

    @Test
    void registerRejectsNullRequestBody() {
        assertThatThrownBy(() -> service.register(null))
            .isInstanceOf(OAuthClientRegistrationException.class)
            .satisfies(ex -> assertThat(((OAuthClientRegistrationException) ex).getErrorCode())
                .isEqualTo("invalid_redirect_uri"));
    }

    @Test
    void registerRejectsMissingRedirectUris() {
        assertThatThrownBy(() -> service.register(requestWithRedirectUris(null)))
            .isInstanceOf(OAuthClientRegistrationException.class);
    }

    @Test
    void registerRejectsEmptyRedirectUris() {
        assertThatThrownBy(() -> service.register(requestWithRedirectUris(List.of())))
            .isInstanceOf(OAuthClientRegistrationException.class);
    }

    @Test
    void registerRejectsRemoteHttpRedirectUri() {
        assertThatThrownBy(() -> service.register(requestWithRedirectUris(List.of("http://evil.example.com/callback"))))
            .isInstanceOf(OAuthClientRegistrationException.class)
            .satisfies(ex -> assertThat(((OAuthClientRegistrationException) ex).getErrorCode())
                .isEqualTo("invalid_redirect_uri"));
    }

    @Test
    void registerRejectsLoopbackRedirectUriWithoutExplicitPort() {
        assertThatThrownBy(() -> service.register(requestWithRedirectUris(List.of("http://localhost/callback"))))
            .isInstanceOf(OAuthClientRegistrationException.class);
    }

    @Test
    void registerRejectsRedirectUriWithFragment() {
        assertThatThrownBy(() -> service.register(
            requestWithRedirectUris(List.of("https://app.example.com/callback#fragment"))))
            .isInstanceOf(OAuthClientRegistrationException.class);
    }

    @Test
    void registerRejectsRedirectUriWithWildcard() {
        assertThatThrownBy(() -> service.register(requestWithRedirectUris(List.of("https://*.example.com/callback"))))
            .isInstanceOf(OAuthClientRegistrationException.class);
    }

    @Test
    void registerRejectsJavascriptSchemeRedirectUri() {
        assertThatThrownBy(() -> service.register(requestWithRedirectUris(List.of("javascript:alert(1)"))))
            .isInstanceOf(OAuthClientRegistrationException.class);
    }

    @Test
    void registerRejectsUnsupportedGrantType() {
        ClientRegistrationRequest request = new ClientRegistrationRequest(
            List.of("https://app.example.com/callback"), null, List.of("client_credentials"), null, null, null, null);

        assertThatThrownBy(() -> service.register(request))
            .isInstanceOf(OAuthClientRegistrationException.class)
            .satisfies(ex -> assertThat(((OAuthClientRegistrationException) ex).getErrorCode())
                .isEqualTo("invalid_client_metadata"));
    }

    @Test
    void registerAcceptsHttpsRedirectUri() {
        ClientRegistrationResponse response = service.register(requestWithRedirectUris(
            List.of("https://app.example.com/callback")));

        assertThat(response.clientId()).startsWith("dcr_");
        assertThat(response.redirectUris()).containsExactly("https://app.example.com/callback");
        verify(repository).save(any(McpOAuthRegisteredClient.class));
    }

    @Test
    void registerAcceptsLoopbackRedirectUriWithExplicitPort() {
        ClientRegistrationResponse response = service.register(
            requestWithRedirectUris(List.of("http://localhost:54321/callback", "http://127.0.0.1:54321/callback")));

        assertThat(response.redirectUris())
            .containsExactly("http://localhost:54321/callback", "http://127.0.0.1:54321/callback");
    }

    @Test
    void registerNeverEmitsClientSecretAndFixesAuthMethodToNone() {
        ClientRegistrationRequest request = new ClientRegistrationRequest(
            List.of("https://app.example.com/callback"), "client_secret_basic", null, null, null, null, null);

        ClientRegistrationResponse response = service.register(request);

        // Sem client_secret_basic: todo cliente DCR é público, o pedido é ignorado.
        assertThat(response.tokenEndpointAuthMethod()).isEqualTo("none");
    }

    @Test
    void registerDefaultsGrantTypesWhenNotRequested() {
        ClientRegistrationResponse response = service.register(requestWithRedirectUris(
            List.of("https://app.example.com/callback")));

        assertThat(response.grantTypes()).containsExactly("authorization_code", "refresh_token");
        assertThat(response.responseTypes()).containsExactly("code");
    }

    @Test
    void registerDefaultsScopeWhenBlank() {
        ClientRegistrationResponse response = service.register(requestWithRedirectUris(
            List.of("https://app.example.com/callback")));

        assertThat(response.scope()).isEqualTo("read read_write");
    }

    @Test
    void registerPersistsClientNameFromRequest() {
        ArgumentCaptor<McpOAuthRegisteredClient> captor = ArgumentCaptor.forClass(McpOAuthRegisteredClient.class);

        service.register(requestWithRedirectUris(List.of("https://app.example.com/callback")));

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getClientName()).isEqualTo("Test Client");
    }

    @Test
    void registerAcceptsIpv6LoopbackRedirectUriWithExplicitPort() {
        ClientRegistrationResponse response = service.register(
            requestWithRedirectUris(List.of("http://[::1]:54321/callback")));

        assertThat(response.redirectUris()).containsExactly("http://[::1]:54321/callback");
    }

    @Test
    void registerRejectsIpv6LoopbackRedirectUriWithoutExplicitPort() {
        assertThatThrownBy(() -> service.register(requestWithRedirectUris(List.of("http://[::1]/callback"))))
            .isInstanceOf(OAuthClientRegistrationException.class)
            .satisfies(ex -> assertThat(((OAuthClientRegistrationException) ex).getErrorCode())
                .isEqualTo("invalid_redirect_uri"));
    }

    @Test
    void registerRejectsOversizedClientName() {
        ClientRegistrationRequest request = new ClientRegistrationRequest(
            List.of("https://app.example.com/callback"), null, null, null, null, "x".repeat(201), null);

        assertThatThrownBy(() -> service.register(request))
            .isInstanceOf(OAuthClientRegistrationException.class)
            .satisfies(ex -> assertThat(((OAuthClientRegistrationException) ex).getErrorCode())
                .isEqualTo("invalid_client_metadata"));
    }

    @Test
    void registerAcceptsClientNameAtExactMaxLength() {
        ClientRegistrationRequest request = new ClientRegistrationRequest(
            List.of("https://app.example.com/callback"), null, null, null, null, "x".repeat(200), null);

        ClientRegistrationResponse response = service.register(request);

        assertThat(response.clientName()).hasSize(200);
    }

    @Test
    void registerRejectsOversizedScope() {
        ClientRegistrationRequest request = new ClientRegistrationRequest(
            List.of("https://app.example.com/callback"), null, null, null, "read ".repeat(15), null, null);

        assertThatThrownBy(() -> service.register(request))
            .isInstanceOf(OAuthClientRegistrationException.class)
            .satisfies(ex -> assertThat(((OAuthClientRegistrationException) ex).getErrorCode())
                .isEqualTo("invalid_client_metadata"));
    }

    @Test
    void registerDeduplicatesRepeatedGrantTypes() {
        ClientRegistrationRequest request = new ClientRegistrationRequest(
            List.of("https://app.example.com/callback"), null,
            List.of("authorization_code", "authorization_code", "refresh_token", "authorization_code"),
            null, null, null, null);

        ClientRegistrationResponse response = service.register(request);

        assertThat(response.grantTypes()).containsExactly("authorization_code", "refresh_token");
    }

    @Test
    void registerRejectsWhenRegisteredClientCapReached() {
        McpOAuthProperties properties = new McpOAuthProperties();
        properties.getDcr().setMaxRegisteredClients(1);
        service = new McpOAuthDynamicClientService(repository, properties);
        when(repository.count()).thenReturn(1L);

        assertThatThrownBy(() -> service.register(requestWithRedirectUris(List.of("https://app.example.com/callback"))))
            .isInstanceOf(OAuthClientRegistrationException.class)
            .satisfies(ex -> assertThat(((OAuthClientRegistrationException) ex).getErrorCode())
                .isEqualTo("temporarily_unavailable"));
    }
}
