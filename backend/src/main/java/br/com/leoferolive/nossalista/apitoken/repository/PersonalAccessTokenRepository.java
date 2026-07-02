package br.com.leoferolive.nossalista.apitoken.repository;

import br.com.leoferolive.nossalista.apitoken.domain.PersonalAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações da entidade {@link PersonalAccessToken}.
 */
@Repository
public interface PersonalAccessTokenRepository extends JpaRepository<PersonalAccessToken, UUID> {

    /**
     * Busca um token pelo hash SHA-256 do valor em claro. Usado na autenticação
     * via PAT — nunca se busca pelo valor em claro, apenas pelo hash.
     *
     * @param tokenHash hash SHA-256 (hex) do token
     * @return Optional contendo o token se encontrado
     */
    Optional<PersonalAccessToken> findByTokenHash(String tokenHash);

    /**
     * Lista os tokens de um usuário, mais recentes primeiro.
     *
     * @param userId ID do usuário dono dos tokens
     * @return lista de tokens (metadados)
     */
    List<PersonalAccessToken> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Busca um token pelo id garantindo que pertence ao usuário informado.
     * Usado para revogação — evita que um usuário revogue token de outro.
     *
     * @param id     ID do token
     * @param userId ID do usuário dono esperado
     * @return Optional contendo o token se encontrado e pertencente ao usuário
     */
    Optional<PersonalAccessToken> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Conta os tokens ainda não revogados de um usuário, usado para aplicar o
     * limite máximo de tokens ativos por conta.
     *
     * @param userId ID do usuário
     * @return quantidade de tokens não revogados (inclui possivelmente expirados)
     */
    long countByUserIdAndRevokedAtIsNull(UUID userId);
}
