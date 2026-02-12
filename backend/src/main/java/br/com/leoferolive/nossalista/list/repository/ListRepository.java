package br.com.leoferolive.nossalista.list.repository;

import br.com.leoferolive.nossalista.list.domain.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações da entidade List
 */
@Repository
public interface ListRepository extends JpaRepository<List, UUID> {

    /**
     * Busca todas as listas de um determinado proprietário.
     *
     * @param ownerId o ID do usuário proprietário
     * @return lista de listas pertencentes ao usuário
     */
    java.util.List<List> findByOwnerId(UUID ownerId);

    /**
     * Busca uma lista pelo código de convite.
     *
     * @param inviteCode o código de convite
     * @return Optional contendo a lista se encontrada
     */
    Optional<List> findByInviteCode(String inviteCode);

    /**
     * Verifica se já existe uma lista com o código de convite fornecido.
     *
     * @param inviteCode o código de convite a verificar
     * @return true se o código já existe, false caso contrário
     */
    boolean existsByInviteCode(String inviteCode);

    // TODO: Adicionar countItems() na Story 3.1 quando ListItem for criado
}
