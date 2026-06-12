package br.com.leoferolive.nossalista.auth.repository;

import br.com.leoferolive.nossalista.auth.domain.OAuthAuthorizationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
     * Remove todas as entradas expiradas (varredura de limpeza).
     *
     * @param cutoff instante-limite; entradas com {@code expires_at} anterior são removidas
     */
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
