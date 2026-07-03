package br.com.leoferolive.nossalista.mcpoauth.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários da entidade {@link PendingAuthorization} — mesmo racional de
 * {@code McpOAuthCodeTest}: o callback {@code @PrePersist} não é exercitado
 * pelos testes de integração no ramo "id já atribuído manualmente".
 */
class PendingAuthorizationTest {

    @Test
    void onCreateGeneratesIdWhenAbsent() {
        PendingAuthorization pending = new PendingAuthorization();
        pending.onCreate();

        assertThat(pending.getId()).isNotNull();
        assertThat(pending.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreatePreservesIdWhenAlreadySet() {
        PendingAuthorization pending = new PendingAuthorization();
        UUID presetId = UUID.randomUUID();
        pending.setId(presetId);

        pending.onCreate();

        assertThat(pending.getId()).isEqualTo(presetId);
    }

    @Test
    void isExpiredReflectsExpiresAt() {
        PendingAuthorization pending = new PendingAuthorization();
        pending.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertThat(pending.isExpired(LocalDateTime.now())).isTrue();

        pending.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        assertThat(pending.isExpired(LocalDateTime.now())).isFalse();
    }
}
