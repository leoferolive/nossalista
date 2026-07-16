package br.com.leoferolive.nossalista.push;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Store de inscrições de push (Web Push), persistido no PostgreSQL via
 * {@link PushSubscriptionRepository} (tabela {@code push_subscriptions}, migration V16).
 *
 * <p><b>Por que persistido e não in-memory:</b> como cada deploy substitui o pod
 * ({@code replicas: 1}), um store {@code ConcurrentHashMap} por instância perdia todas as
 * inscrições a cada release, e push notifications paravam silenciosamente até o usuário
 * reabrir o app e reinscrever. Também não sobrevivia a mais de uma réplica. Mesmo padrão de
 * {@code OAuthCodeStore} (D-011) e {@code password_reset_tokens}. Ver D-028.</p>
 *
 * <p>Este adaptador preserva a API pública original (mesma assinatura de métodos usada por
 * {@code PushController} e {@code PushNotificationService}), que agora delega ao repository
 * em vez de um mapa em memória.</p>
 */
@Component
public class PushSubscriptionStore {

    private static final int MAX_PER_USER = 5;

    private final PushSubscriptionRepository repository;

    public PushSubscriptionStore(PushSubscriptionRepository repository) {
        this.repository = repository;
    }

    /**
     * Adiciona (ou substitui, se o endpoint já existir) uma inscrição de push para o usuário.
     *
     * <p>O endpoint é globalmente único: se já pertencer a outro usuário — por exemplo, o
     * mesmo navegador reinscrevendo após logout/login com outra conta — a inscrição é
     * transferida para o novo dono (upsert por endpoint) em vez de gerar um conflito de
     * unicidade. Mantém apenas as {@value #MAX_PER_USER} inscrições mais recentemente
     * atualizadas por usuário, removendo as mais antigas (FIFO).</p>
     */
    @Transactional
    public void add(UUID userId, PushSubscription subscription) {
        PushSubscriptionEntity entity = repository.findByEndpoint(subscription.endpoint())
            .orElseGet(PushSubscriptionEntity::new);
        entity.setUserId(userId);
        entity.setEndpoint(subscription.endpoint());
        entity.setP256dh(subscription.p256dh());
        entity.setAuth(subscription.auth());
        repository.save(entity);

        List<PushSubscriptionEntity> current = repository.findByUserIdOrderByUpdatedAtAsc(userId);
        if (current.size() > MAX_PER_USER) {
            List<PushSubscriptionEntity> oldest = current.subList(0, current.size() - MAX_PER_USER);
            repository.deleteAll(oldest);
        }
    }

    @Transactional
    public void remove(UUID userId, String endpoint) {
        repository.deleteByUserIdAndEndpoint(userId, endpoint);
    }

    @Transactional
    public void removeAll(UUID userId) {
        repository.deleteByUserId(userId);
    }

    public List<PushSubscription> findByUserId(UUID userId) {
        return repository.findByUserIdOrderByUpdatedAtAsc(userId).stream()
            .map(this::toDto)
            .toList();
    }

    private PushSubscription toDto(PushSubscriptionEntity entity) {
        return new PushSubscription(entity.getEndpoint(), entity.getP256dh(), entity.getAuth());
    }
}
