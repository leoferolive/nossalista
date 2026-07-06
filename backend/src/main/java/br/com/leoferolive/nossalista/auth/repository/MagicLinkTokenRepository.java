package br.com.leoferolive.nossalista.auth.repository;

import br.com.leoferolive.nossalista.auth.domain.MagicLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MagicLinkTokenRepository extends JpaRepository<MagicLinkToken, UUID> {

    Optional<MagicLinkToken> findByTokenAndUsedFalse(String token);

    void deleteByUserIdAndUsedFalse(UUID userId);
}
