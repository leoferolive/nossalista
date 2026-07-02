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
     * Verifica, sem incrementar, se a chave já está bloqueada pelo limite
     * configurado. Usado quando o chamador precisa decidir se bloqueia a
     * requisição ANTES de fazer trabalho adicional (ex.: lookup no banco) —
     * ao contrário de {@link #isAllowed}, não registra tentativa nem cria
     * bucket novo.
     *
     * @param key   chave de rate limiting
     * @param limit número máximo de tentativas na janela
     * @return true se a chave já atingiu ou excedeu o limite na janela corrente
     */
    public boolean isBlocked(String key, int limit) {
        RateBucket bucket = buckets.get(key);
        if (bucket == null || bucket.isExpired(Instant.now())) {
            return false;
        }
        return bucket.currentCount() >= limit;
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

        int currentCount() {
            return count.get();
        }
    }
}
