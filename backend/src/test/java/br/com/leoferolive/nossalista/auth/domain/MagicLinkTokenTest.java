package br.com.leoferolive.nossalista.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MagicLinkToken")
class MagicLinkTokenTest {

    @Test
    @DisplayName("onCreate gera id e createdAt quando ausentes")
    void onCreateGeneratesIdAndTimestampWhenAbsent() {
        MagicLinkToken token = new MagicLinkToken();

        token.onCreate();

        assertThat(token.getId()).isNotNull();
        assertThat(token.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onCreate preserva um id já definido")
    void onCreatePreservesExistingId() {
        MagicLinkToken token = new MagicLinkToken();
        UUID existing = UUID.randomUUID();
        token.setId(existing);

        token.onCreate();

        assertThat(token.getId()).isEqualTo(existing);
    }
}
