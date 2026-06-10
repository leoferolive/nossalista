package br.com.leoferolive.nossalista.auth.repository;

import br.com.leoferolive.nossalista.auth.domain.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações da entidade EmailVerificationToken (Q2.7)
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    /**
     * Busca um token válido (não usado) pelo valor do token
     *
     * @param token valor do token
     * @return Optional contendo o token se encontrado e não usado
     */
    Optional<EmailVerificationToken> findByTokenAndUsedFalse(String token);

    /**
     * Remove todos os tokens não usados de um usuário (limpa tokens antigos)
     *
     * @param userId ID do usuário
     */
    void deleteByUserIdAndUsedFalse(UUID userId);
}
