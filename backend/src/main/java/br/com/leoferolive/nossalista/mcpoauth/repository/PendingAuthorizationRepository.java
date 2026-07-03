package br.com.leoferolive.nossalista.mcpoauth.repository;

import br.com.leoferolive.nossalista.mcpoauth.domain.PendingAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PendingAuthorizationRepository extends JpaRepository<PendingAuthorization, UUID> {

    /**
     * Reivindica o pedido para {@code userId} de forma ATÔMICA: só grava
     * {@code claimed_by_user_id} se ainda estiver {@code NULL}. Sob concorrência
     * (duas contas diferentes chamando {@code view}/{@code approve}/{@code deny}
     * quase simultaneamente para o MESMO pedido ainda não reivindicado), o
     * {@code WHERE claimed_by_user_id IS NULL} garante que só UMA das duas
     * transações grava — a outra recebe 0 e deve então CONFERIR (via
     * {@link #findClaimedByUserId}) se quem venceu foi o mesmo usuário
     * (achado do review, M-1: o padrão anterior de ler em Java e decidir/gravar
     * em passos separados permitia que a segunda chamada sobrescrevesse a
     * reivindicação da primeira, silenciosamente).
     *
     * @return 1 se este chamador reivindicou o pedido agora; 0 se já estava
     *         reivindicado (por este ou por outro usuário, possivelmente em
     *         paralelo)
     */
    @Modifying
    @Query("UPDATE PendingAuthorization p SET p.claimedByUserId = :userId "
        + "WHERE p.id = :id AND p.claimedByUserId IS NULL")
    int claimIfUnclaimed(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Leitura ESCALAR (não a entidade) do dono atual da reivindicação —
     * deliberado: se {@code pending} já estiver no contexto de persistência
     * (carregado antes em {@code requirePending}), um {@code findById} comum
     * devolveria a instância em cache do 1º nível do Hibernate, sem refletir o
     * {@code UPDATE} feito por {@link #claimIfUnclaimed} nesta mesma transação.
     * Uma projeção escalar sempre dispara um SELECT novo contra o banco.
     */
    @Query("SELECT p.claimedByUserId FROM PendingAuthorization p WHERE p.id = :id")
    Optional<UUID> findClaimedByUserId(@Param("id") UUID id);

    @Modifying
    @Query("DELETE FROM PendingAuthorization p WHERE p.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
