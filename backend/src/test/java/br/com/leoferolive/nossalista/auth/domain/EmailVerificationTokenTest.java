package br.com.leoferolive.nossalista.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailVerificationToken")
class EmailVerificationTokenTest {

    @Test
    @DisplayName("onCreate gera id e createdAt quando ausentes")
    void onCreateGeneratesIdAndTimestampWhenAbsent() {
        EmailVerificationToken token = new EmailVerificationToken();

        token.onCreate();

        assertThat(token.getId()).isNotNull();
        assertThat(token.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onCreate preserva um id já definido")
    void onCreatePreservesExistingId() {
        EmailVerificationToken token = new EmailVerificationToken();
        UUID existing = UUID.randomUUID();
        token.setId(existing);

        token.onCreate();

        assertThat(token.getId()).isEqualTo(existing);
    }

    @Test
    @DisplayName("getters refletem os setters")
    void gettersReflectSetters() {
        EmailVerificationToken token = new EmailVerificationToken();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime expires = LocalDateTime.now().plusHours(24);
        LocalDateTime created = LocalDateTime.now();

        token.setId(id);
        token.setUserId(userId);
        token.setToken("verify-token");
        token.setExpiresAt(expires);
        token.setUsed(true);
        token.setCreatedAt(created);

        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getToken()).isEqualTo("verify-token");
        assertThat(token.getExpiresAt()).isEqualTo(expires);
        assertThat(token.isUsed()).isTrue();
        assertThat(token.getCreatedAt()).isEqualTo(created);
    }
}
