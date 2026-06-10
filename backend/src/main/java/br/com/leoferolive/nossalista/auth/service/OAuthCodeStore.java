package br.com.leoferolive.nossalista.auth.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Store in-memory para o padrão "one-time code" do login OAuth2 (Q2.3).
 *
 * <p>No sucesso do OAuth2 o JWT NÃO é mais colocado na URL de redirect — onde
 * vazaria para o histórico do browser, logs de servidor e header {@code Referer}.
 * Em vez disso geramos um código opaco de uso único, guardamos o mapeamento
 * {@code code → JWT} aqui com TTL curto e redirecionamos o frontend apenas com o
 * {@code code}. O frontend troca o code pelo JWT via endpoint dedicado.</p>
 *
 * <p>Características de segurança:</p>
 * <ul>
 *   <li><b>Aleatoriedade forte:</b> {@link SecureRandom} + Base64 URL-safe (256 bits).</li>
 *   <li><b>Single-use:</b> {@link #consume(String)} remove a entrada atomicamente,
 *       então um code só pode ser trocado uma única vez.</li>
 *   <li><b>TTL curto:</b> codes expiram em {@link #DEFAULT_TTL} (60s).</li>
 * </ul>
 *
 * <p>Thread-safe via {@link ConcurrentHashMap}. Entradas expiradas são removidas
 * de forma lazy no consumo e varridas por {@link #evictExpired()} (acionado pelo
 * scheduler de cleanup existente) para evitar vazamento de memória.</p>
 */
@Component
public class OAuthCodeStore {

    /** TTL padrão do code: 60s — janela curta entre redirect e troca. */
    static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    /** 32 bytes = 256 bits de entropia para o code. */
    private static final int CODE_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final long ttlNanos;
    private final ConcurrentMap<String, CodeEntry> codes = new ConcurrentHashMap<>();

    public OAuthCodeStore() {
        this(DEFAULT_TTL);
    }

    OAuthCodeStore(Duration ttl) {
        this.ttlNanos = ttl.toNanos();
    }

    /**
     * Gera um code opaco de uso único associado ao JWT informado.
     *
     * @param jwt token JWT a ser entregue na troca do code
     * @return o code aleatório (Base64 URL-safe) a ser enviado ao frontend
     */
    public String issue(String jwt) {
        byte[] bytes = new byte[CODE_BYTES];
        secureRandom.nextBytes(bytes);
        String code = encoder.encodeToString(bytes);
        codes.put(code, new CodeEntry(jwt, System.nanoTime() + ttlNanos));
        return code;
    }

    /**
     * Consome um code: valida que existe e não expirou, e o invalida (single-use).
     *
     * @param code code recebido do frontend
     * @return Optional com o JWT se o code for válido e não-expirado; vazio caso
     *         contrário (inexistente, já consumido ou expirado)
     */
    public Optional<String> consume(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        CodeEntry entry = codes.remove(code);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired(System.nanoTime())) {
            return Optional.empty();
        }
        return Optional.of(entry.jwt());
    }

    /**
     * Remove todos os codes expirados. Evita acúmulo de codes nunca trocados.
     */
    public void evictExpired() {
        long now = System.nanoTime();
        codes.values().removeIf(entry -> entry.isExpired(now));
    }

    /**
     * Número de codes atualmente mantidos (incluindo possivelmente expirados
     * ainda não varridos). Usado por testes.
     *
     * @return tamanho do store
     */
    int size() {
        return codes.size();
    }

    private record CodeEntry(String jwt, long expiresAtNanos) {
        boolean isExpired(long nowNanos) {
            return nowNanos - expiresAtNanos >= 0;
        }
    }
}
