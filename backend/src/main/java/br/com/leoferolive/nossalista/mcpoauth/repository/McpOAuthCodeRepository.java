package br.com.leoferolive.nossalista.mcpoauth.repository;

import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface McpOAuthCodeRepository extends JpaRepository<McpOAuthCode, UUID> {

    Optional<McpOAuthCode> findByCode(String code);

    /**
     * Consome o code de forma ATÔMICA: só marca {@code consumed_at}/{@code token_family_id}
     * se ainda não estiver consumido. Sob concorrência (duas trocas do MESMO code
     * chegando quase simultaneamente), o {@code WHERE consumed_at IS NULL} garante
     * que só UMA das duas transações afeta uma linha — a outra recebe 0 e deve
     * tratar como replay (revogar a família de tokens), fechando a janela de
     * corrida que existia com o padrão anterior de ler o estado em Java e
     * decidir/gravar em passos separados (achado do review, I-1).
     *
     * @return 1 se este chamador consumiu o code agora; 0 se já estava consumido
     *         (por esta ou por outra transação concorrente)
     */
    @Modifying
    @Query("UPDATE McpOAuthCode c SET c.consumedAt = :now, c.tokenFamilyId = :familyId "
        + "WHERE c.id = :id AND c.consumedAt IS NULL")
    int markConsumed(@Param("id") UUID id, @Param("familyId") UUID familyId, @Param("now") LocalDateTime now);

    /**
     * Leitura ESCALAR (não a entidade) do {@code token_family_id} gravado —
     * deliberado: quando esta é chamada logo após {@link #markConsumed} falhar
     * (retornar 0) na MESMA transação, a entidade já lida antes por
     * {@link #findByCode} está no cache de 1º nível do Hibernate; um
     * {@code findByCode} comum devolveria essa instância cacheada, com
     * {@code tokenFamilyId} desatualizado (nunca mutado em Java, já que o
     * consumo é feito via UPDATE em massa direto no banco). Uma projeção
     * escalar sempre dispara um SELECT novo contra o banco.
     */
    @Query("SELECT c.tokenFamilyId FROM McpOAuthCode c WHERE c.code = :code")
    Optional<UUID> findTokenFamilyIdByCode(@Param("code") String code);

    @Modifying
    @Query("DELETE FROM McpOAuthCode c WHERE c.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
