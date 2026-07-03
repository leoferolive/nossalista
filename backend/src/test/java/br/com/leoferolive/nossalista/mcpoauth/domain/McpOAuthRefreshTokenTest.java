package br.com.leoferolive.nossalista.mcpoauth.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários da entidade {@link McpOAuthRefreshToken}. {@code isActive}
 * não é chamada hoje em lugar nenhum do código de produção (mesma forma que
 * {@link br.com.leoferolive.nossalista.apitoken.domain.PersonalAccessToken#isActive}
 * é usada por {@code PersonalAccessTokenService}, mas o equivalente aqui ainda
 * não tem um chamador) — testado diretamente por já ser uma API pública da
 * entidade, para não deixar uma regra de negócio (token ativo = não revogado
 * e não expirado) sem cobertura alguma.
 */
class McpOAuthRefreshTokenTest {

    private McpOAuthRefreshToken newToken() {
        McpOAuthRefreshToken token = new McpOAuthRefreshToken();
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        return token;
    }

    @Test
    void onCreateGeneratesIdWhenAbsent() {
        McpOAuthRefreshToken token = new McpOAuthRefreshToken();
        token.onCreate();

        assertThat(token.getId()).isNotNull();
        assertThat(token.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreatePreservesIdWhenAlreadySet() {
        McpOAuthRefreshToken token = new McpOAuthRefreshToken();
        UUID presetId = UUID.randomUUID();
        token.setId(presetId);

        token.onCreate();

        assertThat(token.getId()).isEqualTo(presetId);
    }

    @Test
    void isActiveIsTrueWhenNotRevokedAndNotExpired() {
        McpOAuthRefreshToken token = newToken();
        assertThat(token.isActive(LocalDateTime.now())).isTrue();
    }

    @Test
    void isActiveIsFalseWhenRevoked() {
        McpOAuthRefreshToken token = newToken();
        token.setRevokedAt(LocalDateTime.now());

        assertThat(token.isActive(LocalDateTime.now())).isFalse();
    }

    @Test
    void isActiveIsFalseWhenExpired() {
        McpOAuthRefreshToken token = newToken();
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        assertThat(token.isActive(LocalDateTime.now())).isFalse();
    }
}
