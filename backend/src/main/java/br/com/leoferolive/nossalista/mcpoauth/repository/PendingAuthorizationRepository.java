package br.com.leoferolive.nossalista.mcpoauth.repository;

import br.com.leoferolive.nossalista.mcpoauth.domain.PendingAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PendingAuthorizationRepository extends JpaRepository<PendingAuthorization, UUID> {

    @Modifying
    @Query("DELETE FROM PendingAuthorization p WHERE p.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
