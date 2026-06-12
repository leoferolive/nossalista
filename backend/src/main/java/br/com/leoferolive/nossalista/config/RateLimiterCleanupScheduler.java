package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.auth.service.OAuthCodeStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Varredura de limpeza com TTL a cada 5 minutos:
 * buckets expirados do {@link RateLimiterService} (in-memory), entradas
 * expiradas do {@link AuthenticatedUserCache} (in-memory) e one-time codes
 * expirados do {@link OAuthCodeStore} (persistido no banco), evitando
 * acúmulo de lixo (memória / linhas órfãs).
 */
@Component
public class RateLimiterCleanupScheduler {

    private final RateLimiterService rateLimiterService;
    private final AuthenticatedUserCache authenticatedUserCache;
    private final OAuthCodeStore oauthCodeStore;

    public RateLimiterCleanupScheduler(
        RateLimiterService rateLimiterService,
        AuthenticatedUserCache authenticatedUserCache,
        OAuthCodeStore oauthCodeStore
    ) {
        this.rateLimiterService = rateLimiterService;
        this.authenticatedUserCache = authenticatedUserCache;
        this.oauthCodeStore = oauthCodeStore;
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        rateLimiterService.cleanup();
        authenticatedUserCache.evictExpired();
        oauthCodeStore.evictExpired();
    }
}
