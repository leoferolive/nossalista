package br.com.leoferolive.nossalista.mcpoauth.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários da entidade {@link McpOAuthRegisteredClient} — foco na
 * conversão de {@code redirect_uris}/{@code grant_types} entre {@code List<String>}
 * e o texto delimitado persistido (mesmo racional de {@code PendingAuthorizationTest}
 * para o callback {@code @PrePersist}).
 */
class McpOAuthRegisteredClientTest {

    @Test
    void onCreateGeneratesIdWhenAbsent() {
        McpOAuthRegisteredClient client = new McpOAuthRegisteredClient();
        client.onCreate();

        assertThat(client.getId()).isNotNull();
        assertThat(client.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreatePreservesIdWhenAlreadySet() {
        McpOAuthRegisteredClient client = new McpOAuthRegisteredClient();
        UUID presetId = UUID.randomUUID();
        client.setId(presetId);

        client.onCreate();

        assertThat(client.getId()).isEqualTo(presetId);
    }

    @Test
    void redirectUrisRoundTripThroughDelimitedStorage() {
        McpOAuthRegisteredClient client = new McpOAuthRegisteredClient();
        List<String> uris = List.of("https://app.example.com/callback", "http://localhost:54321/callback");

        client.setRedirectUris(uris);

        assertThat(client.getRedirectUris()).containsExactlyElementsOf(uris);
    }

    @Test
    void grantTypesRoundTripThroughDelimitedStorage() {
        McpOAuthRegisteredClient client = new McpOAuthRegisteredClient();
        List<String> grantTypes = List.of("authorization_code", "refresh_token");

        client.setGrantTypes(grantTypes);

        assertThat(client.getGrantTypes()).containsExactlyElementsOf(grantTypes);
    }

    @Test
    void getRedirectUrisReturnsEmptyListWhenNeverSet() {
        McpOAuthRegisteredClient client = new McpOAuthRegisteredClient();

        assertThat(client.getRedirectUris()).isEmpty();
    }
}
