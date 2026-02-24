package br.com.leoferolive.nossalista.member.repository;

import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para operações de persistência de membros de lista
 */
@Repository
public interface ListMemberRepository extends JpaRepository<ListMember, UUID> {

    /**
     * Busca todos os membros de uma lista específica
     *
     * @param listId ID da lista
     * @return Lista de membros
     */
    List<ListMember> findByListId(UUID listId);

    /**
     * Busca todas as listas onde o usuário é membro
     *
     * @param userId ID do usuário
     * @return Lista de associações membro-lista
     */
    List<ListMember> findByUserId(UUID userId);

    /**
     * Verifica se um usuário é membro de uma lista específica
     *
     * @param listId ID da lista
     * @param userId ID do usuário
     * @return true se for membro
     */
    boolean existsByListIdAndUserId(UUID listId, UUID userId);

    /**
     * Busca membro específico de uma lista
     *
     * @param listId ID da lista
     * @param userId ID do usuário
     * @return Optional com o membro se encontrado
     */
    Optional<ListMember> findByListIdAndUserId(UUID listId, UUID userId);

    @Query("""
        SELECT lm
        FROM ListMember lm
        JOIN FETCH lm.user u
        WHERE lm.list.id = :listId
        ORDER BY CASE WHEN lm.role = br.com.leoferolive.nossalista.member.domain.MemberRole.OWNER THEN 0 ELSE 1 END,
                 lm.createdAt ASC
        """)
    java.util.List<ListMember> findMembersForListOrdered(@Param("listId") UUID listId);

    Optional<ListMember> findByListIdAndUserIdAndRole(UUID listId, UUID userId, MemberRole role);
}
