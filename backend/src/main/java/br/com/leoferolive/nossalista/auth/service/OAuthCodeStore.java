package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.OAuthAuthorizationCode;
import br.com.leoferolive.nossalista.auth.repository.OAuthAuthorizationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Store persistido para o padrão "one-time code" do login OAuth2 (Q2.3).
 *
 * <p>No sucesso do OAuth2 o JWT NÃO é colocado na URL de redirect — onde vazaria
 * para histórico do browser, logs e header {@code Referer}. Em vez disso geramos
 * um code opaco de uso único, guardamos o mapeamento {@code code → JWT} e
 * redirecionamos o frontend apenas com o {@code code}, que é trocado pelo JWT via
 * {@code POST /api/auth/oauth/exchange}.</p>
 *
 * <p><b>Por que no banco e não em memória:</b> o code é emitido na requisição de
 * callback do Google e trocado numa segunda requisição (XHR do SPA). Com o store
 * em memória por instância, essas duas requisições caindo em instâncias
 * diferentes (≥1 réplica/HPA) — ou um restart do pod entre elas — faziam o
 * exchange responder 400 e o login Google nunca completar. Persistindo no banco
 * compartilhado, qualquer instância valida o code e o fluxo sobrevive a
 * restart/escala. Mesmo padrão de {@code password_reset_tokens}.</p>
 *
 * <p>Características de segurança preservadas: aleatoriedade forte
 * ({@link SecureRandom} + Base64 URL-safe, 256 bits), single-use (a entrada é
 * removida na troca) e TTL curto ({@link #DEFAULT_TTL}).</p>
 */
@Component
public class OAuthCodeStore {

    /** TTL padrão do code: 60s — janela curta entre redirect e troca. */
    static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    /** 32 bytes = 256 bits de entropia para o code. */
    private static final int CODE_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final OAuthAuthorizationCodeRepository repository;
    private final Duration ttl;

    @Autowired
    public OAuthCodeStore(OAuthAuthorizationCodeRepository repository) {
        this(repository, DEFAULT_TTL);
    }

    OAuthCodeStore(OAuthAuthorizationCodeRepository repository, Duration ttl) {
        this.repository = repository;
        this.ttl = ttl;
    }

    /**
     * Gera um code opaco de uso único associado ao JWT informado e o persiste.
     *
     * @param jwt token JWT a ser entregue na troca do code
     * @return o code aleatório (Base64 URL-safe) a ser enviado ao frontend
     */
    @Transactional
    public String issue(String jwt) {
        byte[] bytes = new byte[CODE_BYTES];
        secureRandom.nextBytes(bytes);
        String code = encoder.encodeToString(bytes);

        OAuthAuthorizationCode entity = new OAuthAuthorizationCode();
        entity.setCode(code);
        entity.setJwt(jwt);
        entity.setExpiresAt(LocalDateTime.now().plus(ttl));
        repository.save(entity);

        return code;
    }

    /**
     * Consome um code: valida que existe e não expirou, e o remove (single-use).
     *
     * @param code code recebido do frontend
     * @return Optional com o JWT se o code for válido e não-expirado; vazio caso
     *         contrário (inexistente, já consumido ou expirado)
     */
    @Transactional
    public Optional<String> consume(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        Optional<OAuthAuthorizationCode> found = repository.findByCode(code);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        OAuthAuthorizationCode entity = found.get();
        // Single-use: remove imediatamente, antes mesmo de checar expiração.
        repository.delete(entity);
        repository.flush();

        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(entity.getJwt());
    }

    /**
     * Remove todos os codes expirados. Evita acúmulo de codes nunca trocados.
     */
    @Transactional
    public void evictExpired() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    /**
     * Número de codes atualmente persistidos (incluindo possivelmente expirados
     * ainda não varridos). Usado por testes.
     *
     * @return tamanho do store
     */
    long size() {
        return repository.count();
    }
}
