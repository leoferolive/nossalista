package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimiterService")
class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    @DisplayName("deve permitir requisições dentro do limite")
    void allowsRequestsWithinLimit() {
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiterService.isAllowed("test-key", 5, Duration.ofMinutes(1))).isTrue();
        }
    }

    @Test
    @DisplayName("deve bloquear requisições acima do limite")
    void blocksRequestsAboveLimit() {
        for (int i = 0; i < 3; i++) {
            rateLimiterService.isAllowed("test-key", 3, Duration.ofMinutes(1));
        }
        assertThat(rateLimiterService.isAllowed("test-key", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("deve isolar chaves diferentes")
    void isolatesDifferentKeys() {
        for (int i = 0; i < 3; i++) {
            rateLimiterService.isAllowed("key-a", 3, Duration.ofMinutes(1));
        }
        assertThat(rateLimiterService.isAllowed("key-a", 3, Duration.ofMinutes(1))).isFalse();
        assertThat(rateLimiterService.isAllowed("key-b", 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    @DisplayName("deve resetar após expiração da janela")
    void resetsAfterWindowExpiration() {
        for (int i = 0; i < 3; i++) {
            rateLimiterService.isAllowed("expire-key", 3, Duration.ofMillis(1));
        }
        // Janela de 1ms já expirou
        try { Thread.sleep(5); } catch (InterruptedException ignored) {}
        assertThat(rateLimiterService.isAllowed("expire-key", 3, Duration.ofMillis(1))).isTrue();
    }

    @Test
    @DisplayName("isBlocked retorna false para chave nunca usada")
    void isBlockedReturnsFalseForUnknownKey() {
        assertThat(rateLimiterService.isBlocked("never-used", 3)).isFalse();
    }

    @Test
    @DisplayName("isBlocked retorna false abaixo do limite e não incrementa")
    void isBlockedDoesNotIncrementAndReturnsFalseBelowLimit() {
        rateLimiterService.isAllowed("peek-key", 3, Duration.ofMinutes(1));

        assertThat(rateLimiterService.isBlocked("peek-key", 3)).isFalse();
        assertThat(rateLimiterService.isBlocked("peek-key", 3)).isFalse();

        // isAllowed ainda tem 2 tentativas livres — isBlocked não consumiu nenhuma.
        assertThat(rateLimiterService.isAllowed("peek-key", 3, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiterService.isAllowed("peek-key", 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    @DisplayName("isBlocked retorna true quando a chave atingiu o limite")
    void isBlockedReturnsTrueAtLimit() {
        for (int i = 0; i < 3; i++) {
            rateLimiterService.isAllowed("blocked-key", 3, Duration.ofMinutes(1));
        }

        assertThat(rateLimiterService.isBlocked("blocked-key", 3)).isTrue();
    }

    @Test
    @DisplayName("isBlocked retorna false após expiração da janela")
    void isBlockedReturnsFalseAfterWindowExpiration() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            rateLimiterService.isAllowed("expire-blocked", 3, Duration.ofMillis(1));
        }
        Thread.sleep(5);

        assertThat(rateLimiterService.isBlocked("expire-blocked", 3)).isFalse();
    }

    @Test
    @DisplayName("cleanup deve remover buckets expirados")
    void cleanupRemovesExpiredBuckets() {
        rateLimiterService.isAllowed("expired", 1, Duration.ofMillis(1));
        rateLimiterService.isAllowed("active", 1, Duration.ofHours(1));

        try { Thread.sleep(5); } catch (InterruptedException ignored) {}
        rateLimiterService.cleanup();

        assertThat(rateLimiterService.bucketCount()).isEqualTo(1);
    }
}
