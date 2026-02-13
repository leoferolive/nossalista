package br.com.leoferolive.nossalista.list.repository;

import br.com.leoferolive.nossalista.list.domain.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Busca todas as listas onde o usuário é owner OU member.
     * Usa LEFT JOIN com list_members para incluir listas compartilhadas.
     *
     * @param userId o ID do usuário
     * @return lista de listas onde o usuário é owner ou membro, ordenadas por updatedAt DESC
     */
    @Query("SELECT DISTINCT l FROM lists l " +
           "JOIN FETCH l.owner o " +
           "JOIN FETCH l.typeEntity t " +
           "LEFT JOIN ListMember lm ON lm.list.id = l.id AND lm.user.id = :userId " +
           "WHERE l.owner.id = :userId OR lm.user.id = :userId " +
           "ORDER BY l.updatedAt DESC")
    java.util.List<List> findAllByOwnerOrMemberOrderByUpdatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT l FROM lists l " +
           "JOIN FETCH l.owner o " +
           "JOIN FETCH l.typeEntity t " +
           "WHERE l.id = :listId")
    Optional<List> findByIdWithDetails(@Param("listId") UUID listId);

    // TODO: Adicionar countItems() na Story 3.1 quando ListItem for criado
}
