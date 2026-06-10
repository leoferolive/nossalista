package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthenticatedUserCache")
class AuthenticatedUserCacheTest {

    private UserService userService;
    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        user = new User();
        user.setId(userId);
        user.setUsername("user");
    }

    @Test
    @DisplayName("segundo request usa cache (não consulta o UserService de novo)")
    void secondRequestHitsCache() {
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        AuthenticatedUserCache cache =
            new AuthenticatedUserCache(userService, Duration.ofSeconds(60));

        assertThat(cache.findById(userId)).contains(user);
        assertThat(cache.findById(userId)).contains(user);

        // Apenas um lookup no banco apesar de dois acessos
        verify(userService, times(1)).findById(userId);
    }

    @Test
    @DisplayName("cache expira após o TTL e reconsulta o UserService")
    void cacheExpiresAfterTtl() throws InterruptedException {
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        AuthenticatedUserCache cache =
            new AuthenticatedUserCache(userService, Duration.ofMillis(20));

        assertThat(cache.findById(userId)).contains(user);
        Thread.sleep(40); // ultrapassa o TTL de 20ms
        assertThat(cache.findById(userId)).contains(user);

        // Dois lookups: o primeiro e o pós-expiração
        verify(userService, times(2)).findById(userId);
    }

    @Test
    @DisplayName("não cacheia usuário inexistente")
    void doesNotCacheMissingUser() {
        when(userService.findById(userId)).thenReturn(Optional.empty());
        AuthenticatedUserCache cache =
            new AuthenticatedUserCache(userService, Duration.ofSeconds(60));

        assertThat(cache.findById(userId)).isEmpty();
        assertThat(cache.findById(userId)).isEmpty();

        // Sem cache de miss: dois lookups
        verify(userService, times(2)).findById(userId);
        assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("invalidate remove a entrada e força nova consulta")
    void invalidateEvictsEntry() {
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        AuthenticatedUserCache cache =
            new AuthenticatedUserCache(userService, Duration.ofSeconds(60));

        cache.findById(userId);
        cache.invalidate(userId);
        cache.findById(userId);

        verify(userService, times(2)).findById(userId);
    }

    @Test
    @DisplayName("evictExpired remove entradas expiradas (não vaza memória)")
    void evictExpiredRemovesStaleEntries() throws InterruptedException {
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        AuthenticatedUserCache cache =
            new AuthenticatedUserCache(userService, Duration.ofMillis(10));

        cache.findById(userId);
        assertThat(cache.size()).isEqualTo(1);

        Thread.sleep(30);
        cache.evictExpired();

        assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("evictExpired preserva entradas válidas")
    void evictExpiredKeepsFreshEntries() {
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        AuthenticatedUserCache cache =
            new AuthenticatedUserCache(userService, Duration.ofSeconds(60));

        cache.findById(userId);
        cache.evictExpired();

        assertThat(cache.size()).isEqualTo(1);
        verify(userService, never()).findById(UUID.randomUUID());
    }
}
