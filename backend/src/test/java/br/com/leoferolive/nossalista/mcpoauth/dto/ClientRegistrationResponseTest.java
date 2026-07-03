package br.com.leoferolive.nossalista.mcpoauth.dto;

import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRegisteredClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste unitário de {@link ClientRegistrationResponse#from} — achado do review
 * (M-3): {@code client_id_issued_at} é um Unix timestamp (instante absoluto,
 * sempre UTC), mas {@code createdAt} é um {@link LocalDateTime} de parede
 * capturado no fuso REAL da JVM, nunca necessariamente UTC. Converter com
 * {@code toEpochSecond(ZoneOffset.UTC)} reinterpreta essa hora de parede COMO
 * SE já fosse UTC — produz o epoch errado sempre que a JVM não roda com
 * {@code TZ=UTC}.
 */
class ClientRegistrationResponseTest {

    @Test
    void clientIdIssuedAtUsesJvmDefaultZoneNotHardcodedUtc() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 3, 12, 0, 0);
        McpOAuthRegisteredClient entity = new McpOAuthRegisteredClient();
        entity.setClientId("dcr_test");
        entity.setCreatedAt(createdAt);
        entity.setRedirectUris(List.of("https://app.example.com/callback"));
        entity.setScope("read read_write");
        entity.setTokenEndpointAuthMethod("none");
        entity.setGrantTypes(List.of("authorization_code", "refresh_token"));

        ClientRegistrationResponse response = ClientRegistrationResponse.from(entity);

        long expectedEpoch = createdAt.atZone(ZoneId.systemDefault()).toEpochSecond();
        assertThat(response.clientIdIssuedAt()).isEqualTo(expectedEpoch);
    }

    @Test
    void clientIdIssuedAtDiffersFromHardcodedUtcConversionWhenJvmZoneIsNotUtc() {
        // Este teste só é significativo em fusos != UTC — pula silenciosamente
        // (via early return, sem @DisabledIf que exigiria uma dependência extra)
        // se a JVM já estiver rodando em UTC, para não ficar falso-positivo em CI
        // configurado com TZ=UTC.
        if (ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now()).equals(ZoneOffset.UTC)) {
            return;
        }
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 3, 12, 0, 0);
        McpOAuthRegisteredClient entity = new McpOAuthRegisteredClient();
        entity.setClientId("dcr_test");
        entity.setCreatedAt(createdAt);
        entity.setRedirectUris(List.of("https://app.example.com/callback"));
        entity.setScope("read read_write");
        entity.setTokenEndpointAuthMethod("none");
        entity.setGrantTypes(List.of("authorization_code", "refresh_token"));

        ClientRegistrationResponse response = ClientRegistrationResponse.from(entity);

        long buggyUtcEpoch = createdAt.toEpochSecond(ZoneOffset.UTC);
        assertThat(response.clientIdIssuedAt()).isNotEqualTo(buggyUtcEpoch);
    }
}
