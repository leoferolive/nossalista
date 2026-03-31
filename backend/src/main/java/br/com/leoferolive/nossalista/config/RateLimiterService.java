package br.com.leoferolive.nossalista.config;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter in-memory baseado em janela fixa.
 * Suficiente para MVP single-instance; para multi-instance usar Redis.
 */
@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, RateBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Verifica se a ação é permitida para a chave dada.
     *
     * @param key     chave de rate limiting (ex: "forgot-password:email:user@test.com")
     * @param limit   número máximo de requisições na janela
     * @param window  duração da janela
     * @return true se permitido, false se excedeu o limite
     */
    public boolean isAllowed(String key, int limit, Duration window) {
        Instant now = Instant.now();
        RateBucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return new RateBucket(now.plus(window));
            }
            return existing;
        });
        return bucket.increment() <= limit;
    }

    /**
     * Remove buckets expirados para evitar memory leak.
     * Chamado periodicamente pelo scheduler.
     */
    public void cleanup() {
        Instant now = Instant.now();
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    /**
     * Remove todos os buckets. Usado em testes.
     */
    public void reset() {
        buckets.clear();
    }

    int bucketCount() {
        return buckets.size();
    }

    private static class RateBucket {
        private final Instant expiresAt;
        private final AtomicInteger count = new AtomicInteger(0);

        RateBucket(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }

        boolean isExpired(Instant now) {
            return now.isAfter(expiresAt);
        }

        int increment() {
            return count.incrementAndGet();
        }
    }
}
