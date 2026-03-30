package br.com.leoferolive.nossalista.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limpa buckets expirados do RateLimiterService a cada 5 minutos.
 */
@Component
public class RateLimiterCleanupScheduler {

    private final RateLimiterService rateLimiterService;

    public RateLimiterCleanupScheduler(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        rateLimiterService.cleanup();
    }
}
