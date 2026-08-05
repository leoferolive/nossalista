package br.com.leoferolive.nossalista.push;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações da entidade {@link PushSubscriptionEntity}.
 */
@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, UUID> {

    /**
     * Busca todas as inscrições de um usuário, da menos para a mais recentemente
     * atualizada (usado pelo {@link PushSubscriptionStore} para aplicar o limite
     * por usuário removendo as mais antigas primeiro).
     *
     * @param userId ID do usuário
     * @return inscrições ordenadas por {@code updatedAt} ascendente
     */
    List<PushSubscriptionEntity> findByUserIdOrderByUpdatedAtAsc(UUID userId);

    /**
     * Busca uma inscrição pelo endpoint (globalmente único).
     *
     * @param endpoint endpoint do Web Push
     * @return Optional contendo a inscrição, se existir
     */
    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    /**
     * Remove a inscrição de um usuário identificada pelo endpoint.
     *
     * @param userId   ID do usuário
     * @param endpoint endpoint do Web Push
     */
    void deleteByUserIdAndEndpoint(UUID userId, String endpoint);

    /**
     * Remove todas as inscrições de um usuário.
     *
     * @param userId ID do usuário
     */
    void deleteByUserId(UUID userId);
}
