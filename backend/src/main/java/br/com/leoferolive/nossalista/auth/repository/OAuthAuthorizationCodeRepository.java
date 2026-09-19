package br.com.leoferolive.nossalista.auth.repository;

import br.com.leoferolive.nossalista.auth.domain.OAuthAuthorizationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório dos one-time codes do login OAuth2 (Q2.3).
 */
@Repository
public interface OAuthAuthorizationCodeRepository extends JpaRepository<OAuthAuthorizationCode, UUID> {

    /**
     * Busca um code pelo seu valor opaco.
     *
     * @param code code recebido do frontend
     * @return Optional com a entrada se existir
     */
    Optional<OAuthAuthorizationCode> findByCode(String code);

    /**
     * Localiza uma linha nova pelo digest do code apresentado pelo cliente.
     *
     * @param codeHash SHA-256 hexadecimal do code opaco
     * @return linha correspondente, se existir
     */
    Optional<OAuthAuthorizationCode> findByCodeHash(String codeHash);

    /**
     * Marca uma linha hash-only como consumida em uma única operação condicional.
     *
     * @return 1 somente para o processo que venceu a reivindicação
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE OAuthAuthorizationCode c SET c.consumedAt = :now "
        + "WHERE c.codeHash = :codeHash AND c.consumedAt IS NULL AND c.expiresAt > :now")
    int claimByCodeHash(@Param("codeHash") String codeHash, @Param("now") LocalDateTime now);

    /**
     * Mantém o mesmo consumo atômico para linhas legadas durante o rollout.
     *
     * @return 1 somente para o processo que venceu a reivindicação
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE OAuthAuthorizationCode c SET c.consumedAt = :now "
        + "WHERE c.code = :code AND c.consumedAt IS NULL AND c.expiresAt > :now")
    int claimLegacyCode(@Param("code") String code, @Param("now") LocalDateTime now);

    /**
     * Remove todas as entradas expiradas (varredura de limpeza).
     *
     * @param cutoff instante-limite; entradas com {@code expires_at} anterior são removidas
     */
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
