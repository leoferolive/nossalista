package br.com.leoferolive.nossalista.mcpoauth.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários da entidade {@link McpOAuthCode} — o callback JPA
 * {@code @PrePersist} e os predicados de expiração/consumo não são exercitados
 * diretamente pelos testes de integração (que só observam o comportamento via
 * HTTP), deixando o ramo "id já atribuído manualmente" de {@code onCreate()}
 * sem cobertura.
 */
class McpOAuthCodeTest {

    @Test
    void onCreateGeneratesIdWhenAbsent() {
        McpOAuthCode code = new McpOAuthCode();
        code.onCreate();

        assertThat(code.getId()).isNotNull();
        assertThat(code.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreatePreservesIdWhenAlreadySet() {
        McpOAuthCode code = new McpOAuthCode();
        UUID presetId = UUID.randomUUID();
        code.setId(presetId);

        code.onCreate();

        assertThat(code.getId()).isEqualTo(presetId);
    }

    @Test
    void isExpiredReflectsExpiresAt() {
        McpOAuthCode code = new McpOAuthCode();
        code.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertThat(code.isExpired(LocalDateTime.now())).isTrue();

        code.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        assertThat(code.isExpired(LocalDateTime.now())).isFalse();
    }

    @Test
    void isConsumedReflectsConsumedAt() {
        McpOAuthCode code = new McpOAuthCode();
        assertThat(code.isConsumed()).isFalse();

        code.setConsumedAt(LocalDateTime.now());
        assertThat(code.isConsumed()).isTrue();
    }
}
