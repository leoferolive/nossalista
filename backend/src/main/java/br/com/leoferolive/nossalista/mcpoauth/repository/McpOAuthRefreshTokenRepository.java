package br.com.leoferolive.nossalista.mcpoauth.repository;

import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface McpOAuthRefreshTokenRepository extends JpaRepository<McpOAuthRefreshToken, UUID> {

    Optional<McpOAuthRefreshToken> findByTokenHash(String tokenHash);

    List<McpOAuthRefreshToken> findByFamilyId(UUID familyId);

    /**
     * Conexões ativas (ao menos um refresh token não revogado e não expirado)
     * de um usuário, uma linha mais recente por família — usado pela tela
     * "Conexões" para listar assistentes conectados via OAuth.
     */
    @Query("SELECT r FROM McpOAuthRefreshToken r WHERE r.userId = :userId AND r.revokedAt IS NULL "
        + "AND r.expiresAt > :now ORDER BY r.createdAt DESC")
    List<McpOAuthRefreshToken> findActiveByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE McpOAuthRefreshToken r SET r.revokedAt = :now WHERE r.familyId = :familyId AND r.revokedAt IS NULL")
    void revokeFamily(@Param("familyId") UUID familyId, @Param("now") LocalDateTime now);

    /**
     * Rotaciona (revoga) o refresh token de forma ATÔMICA: só marca
     * {@code revoked_at}/{@code last_used_at} se ainda não estiver revogado. Sob
     * concorrência (duas rotações do MESMO refresh token quase simultâneas), o
     * {@code WHERE revoked_at IS NULL} garante que só UMA das duas transações
     * afeta uma linha — a outra recebe 0 e deve tratar como reuso (revogar a
     * família inteira), mesmo padrão de
     * {@link br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthCodeRepository#markConsumed}
     * aplicado ao authorization code (achado do review, M-1).
     *
     * @return 1 se este chamador revogou o token agora (rotação legítima); 0 se
     *         já estava revogado (por esta ou por outra transação concorrente)
     */
    @Modifying
    @Query("UPDATE McpOAuthRefreshToken r SET r.revokedAt = :now, r.lastUsedAt = :now "
        + "WHERE r.id = :id AND r.revokedAt IS NULL")
    int markRotated(@Param("id") UUID id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE McpOAuthRefreshToken r SET r.revokedAt = :now WHERE r.userId = :userId "
        + "AND r.clientId = :clientId AND r.revokedAt IS NULL")
    void revokeAllForUserAndClient(
        @Param("userId") UUID userId, @Param("clientId") String clientId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM McpOAuthRefreshToken r WHERE r.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
