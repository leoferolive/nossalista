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

    @Modifying
    @Query("UPDATE McpOAuthRefreshToken r SET r.revokedAt = :now WHERE r.userId = :userId "
        + "AND r.clientId = :clientId AND r.revokedAt IS NULL")
    void revokeAllForUserAndClient(
        @Param("userId") UUID userId, @Param("clientId") String clientId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM McpOAuthRefreshToken r WHERE r.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
