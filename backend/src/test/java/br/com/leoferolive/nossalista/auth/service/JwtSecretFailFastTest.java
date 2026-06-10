package br.com.leoferolive.nossalista.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica o fail-fast do {@link JwtService}: o bean (e portanto o contexto da
 * aplicação) não inicializa quando o secret JWT é ausente, placeholder inseguro
 * ou curto demais para HS256. Usa {@link ApplicationContextRunner} para isolar
 * apenas o bean do JwtService, sem subir o contexto inteiro.
 */
@DisplayName("JwtService fail-fast de inicialização")
class JwtSecretFailFastTest {

    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withUserConfiguration(JwtServiceConfig.class);

    @Test
    @DisplayName("contexto falha quando JWT_SECRET é o placeholder inseguro")
    void contextFailsWithInsecurePlaceholder() {
        contextRunner
            .withPropertyValues(
                "jwt.secret=change-this-secret-key-in-production-minimum-32-characters-for-hs256")
            .run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder"));
    }

    @Test
    @DisplayName("contexto falha quando JWT_SECRET tem menos de 32 bytes")
    void contextFailsWithShortSecret() {
        contextRunner
            .withPropertyValues("jwt.secret=too-short")
            .run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("curto"));
    }

    @Test
    @DisplayName("contexto falha quando JWT_SECRET está ausente/vazio")
    void contextFailsWithMissingSecret() {
        contextRunner
            .withPropertyValues("jwt.secret=")
            .run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ausente"));
    }

    @Test
    @DisplayName("contexto sobe com JWT_SECRET válido (>= 32 bytes)")
    void contextStartsWithValidSecret() {
        contextRunner
            .withPropertyValues(
                "jwt.secret=valid-secret-key-with-at-least-32-bytes-of-entropy")
            .run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(JwtService.class));
    }

    @Configuration
    static class JwtServiceConfig {
        @Bean
        JwtService jwtService() {
            return new JwtService();
        }
    }
}
