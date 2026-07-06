package br.com.leoferolive.nossalista.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordResetToken")
class PasswordResetTokenTest {

    @Test
    @DisplayName("onCreate gera id e createdAt quando ausentes")
    void onCreateGeneratesIdAndTimestampWhenAbsent() {
        PasswordResetToken token = new PasswordResetToken();

        token.onCreate();

        assertThat(token.getId()).isNotNull();
        assertThat(token.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onCreate preserva um id já definido")
    void onCreatePreservesExistingId() {
        PasswordResetToken token = new PasswordResetToken();
        UUID existing = UUID.randomUUID();
        token.setId(existing);

        token.onCreate();

        assertThat(token.getId()).isEqualTo(existing);
    }

    @Test
    @DisplayName("getters refletem os setters")
    void gettersReflectSetters() {
        PasswordResetToken token = new PasswordResetToken();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime expires = LocalDateTime.now().plusHours(1);
        LocalDateTime created = LocalDateTime.now();

        token.setId(id);
        token.setUserId(userId);
        token.setToken("reset-token");
        token.setExpiresAt(expires);
        token.setUsed(true);
        token.setCreatedAt(created);

        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getToken()).isEqualTo("reset-token");
        assertThat(token.getExpiresAt()).isEqualTo(expires);
        assertThat(token.isUsed()).isTrue();
        assertThat(token.getCreatedAt()).isEqualTo(created);
    }
}
