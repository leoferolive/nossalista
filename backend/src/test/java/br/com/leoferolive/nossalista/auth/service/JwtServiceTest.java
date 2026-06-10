package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String VALID_SECRET =
        "valid-test-secret-key-with-more-than-32-bytes-for-hs256";

    private JwtService newServiceWithSecret(String secret) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        ReflectionTestUtils.setField(service, "expirationMs", 3_600_000L);
        return service;
    }

    @Nested
    @DisplayName("validação de secret na inicialização (fail-fast)")
    class SecretValidation {

        @Test
        @DisplayName("deve falhar quando o secret é null")
        void failsWhenSecretIsNull() {
            JwtService service = newServiceWithSecret(null);
            assertThatThrownBy(service::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        }

        @Test
        @DisplayName("deve falhar quando o secret é vazio/em branco")
        void failsWhenSecretIsBlank() {
            JwtService service = newServiceWithSecret("   ");
            assertThatThrownBy(service::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ausente");
        }

        @Test
        @DisplayName("deve falhar quando o secret é o placeholder inseguro")
        void failsWhenSecretIsInsecurePlaceholder() {
            JwtService service = newServiceWithSecret(
                "change-this-secret-key-in-production-minimum-32-characters-for-hs256");
            assertThatThrownBy(service::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
        }

        @Test
        @DisplayName("deve falhar quando o secret tem menos de 32 bytes")
        void failsWhenSecretTooShort() {
            JwtService service = newServiceWithSecret("short-secret-31-bytes-only-here");
            assertThatThrownBy(service::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("curto");
        }

        @Test
        @DisplayName("deve subir com um secret válido (>= 32 bytes)")
        void succeedsWithValidSecret() {
            JwtService service = newServiceWithSecret(VALID_SECRET);
            assertThatCode(service::validateSecret).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("deve gerar e validar token com secret válido")
    void generatesAndValidatesToken() {
        JwtService service = newServiceWithSecret(VALID_SECRET);
        service.validateSecret();

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setUsername("user");

        String token = service.generateToken(user);

        assertThat(service.validateToken(token)).isTrue();
        assertThat(service.extractUserId(token)).isEqualTo(userId);
    }
}
