package br.com.leoferolive.nossalista.mcpoauth.repository;

import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRegisteredClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface McpOAuthRegisteredClientRepository extends JpaRepository<McpOAuthRegisteredClient, UUID> {

    Optional<McpOAuthRegisteredClient> findByClientId(String clientId);

    @Modifying
    @Query("UPDATE McpOAuthRegisteredClient c SET c.lastUsedAt = :now WHERE c.clientId = :clientId")
    void touchLastUsedAt(@Param("clientId") String clientId, @Param("now") LocalDateTime now);

    /**
     * Remove clientes dinâmicos que NUNCA completaram um fluxo authorize->token
     * ({@code last_used_at IS NULL}) e foram registrados antes de {@code cutoff} —
     * varrido por {@code McpOAuthCleanupScheduler} (TTL de cliente não usado,
     * {@code app.mcp-oauth.dcr.unused-client-ttl}). Clientes já usados alguma
     * vez NUNCA são removidos por esta varredura, mesmo sem uso recente.
     */
    @Modifying
    @Query("DELETE FROM McpOAuthRegisteredClient c WHERE c.lastUsedAt IS NULL AND c.createdAt < :cutoff")
    void deleteUnusedRegisteredBefore(@Param("cutoff") LocalDateTime cutoff);
}
