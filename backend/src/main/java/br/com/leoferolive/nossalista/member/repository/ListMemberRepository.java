package br.com.leoferolive.nossalista.member.repository;

import br.com.leoferolive.nossalista.member.domain.ListMember;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
