package br.com.leoferolive.nossalista.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limpa estruturas in-memory com TTL a cada 5 minutos:
 * buckets expirados do {@link RateLimiterService} e entradas expiradas do
 * {@link AuthenticatedUserCache}, evitando vazamento de memória.
 */
@Component
public class RateLimiterCleanupScheduler {

    private final RateLimiterService rateLimiterService;
    private final AuthenticatedUserCache authenticatedUserCache;

    public RateLimiterCleanupScheduler(
        RateLimiterService rateLimiterService,
        AuthenticatedUserCache authenticatedUserCache
    ) {
        this.rateLimiterService = rateLimiterService;
        this.authenticatedUserCache = authenticatedUserCache;
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        rateLimiterService.cleanup();
        authenticatedUserCache.evictExpired();
    }
}
