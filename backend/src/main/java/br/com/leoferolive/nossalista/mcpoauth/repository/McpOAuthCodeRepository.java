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

    @Modifying
    @Query("DELETE FROM McpOAuthCode c WHERE c.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
