package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cache in-memory com TTL curto para o lookup do usuário autenticado.
 *
 * <p>O {@link JwtAuthenticationFilter} resolvia o usuário no banco em TODA
 * requisição autenticada. Este cache reduz a pressão no banco mantendo o
 * resultado por uma janela curta (TTL), preservando a revogação eventual:
 * uma alteração de role ou remoção de usuário passa a valer após o TTL.</p>
 *
 * <p>Thread-safe via {@link ConcurrentHashMap}. Entradas expiradas são
 * removidas de forma lazy no acesso e, ainda, varridas por
 * {@link #evictExpired()} para evitar vazamento de memória de chaves que
 * nunca mais são consultadas.</p>
 */
@Component
public class AuthenticatedUserCache {

    /** TTL padrão do cache: 60s — janela curta para revogação eventual. */
    static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    private final UserService userService;
    private final long ttlNanos;
    private final ConcurrentMap<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public AuthenticatedUserCache(UserService userService) {
        this(userService, DEFAULT_TTL);
    }

    AuthenticatedUserCache(UserService userService, Duration ttl) {
        this.userService = userService;
        this.ttlNanos = ttl.toNanos();
    }

    /**
     * Resolve o usuário pelo id, servindo do cache quando a entrada ainda é
     * válida e delegando ao {@link UserService} (e populando o cache) caso
     * contrário. Usuários inexistentes não são cacheados.
     *
     * @param userId id do usuário extraído do token
     * @return Optional com o usuário, ou vazio se não existir
     */
    public Optional<User> findById(UUID userId) {
        long now = System.nanoTime();
        CacheEntry entry = cache.get(userId);
        if (entry != null && !entry.isExpired(now)) {
            return Optional.of(entry.user());
        }

        Optional<User> loaded = userService.findById(userId);
        loaded.ifPresent(user -> cache.put(userId, new CacheEntry(user, now + ttlNanos)));
        if (loaded.isEmpty()) {
            cache.remove(userId);
        }
        return loaded;
    }

    /**
     * Invalida a entrada de cache de um usuário específico.
     *
     * @param userId id do usuário a invalidar
     */
    public void invalidate(UUID userId) {
        cache.remove(userId);
    }

    /**
     * Remove todas as entradas expiradas. Evita acúmulo de chaves órfãs.
     */
    public void evictExpired() {
        long now = System.nanoTime();
        cache.values().removeIf(entry -> entry.isExpired(now));
    }

    /**
     * Número de entradas atualmente mantidas (incluindo possivelmente
     * expiradas ainda não varridas). Usado por testes.
     *
     * @return tamanho do cache
     */
    int size() {
        return cache.size();
    }

    private record CacheEntry(User user, long expiresAtNanos) {
        boolean isExpired(long nowNanos) {
            return nowNanos - expiresAtNanos >= 0;
        }
    }
}
