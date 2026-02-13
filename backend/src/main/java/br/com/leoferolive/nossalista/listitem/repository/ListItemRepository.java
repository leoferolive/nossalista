package br.com.leoferolive.nossalista.listitem.repository;

import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListItemRepository extends JpaRepository<ListItem, UUID> {

    /**
     * Busca todos os itens de uma lista ordenados por position ASC.
     */
    List<ListItem> findByListIdOrderByPositionAsc(UUID listId);

    /**
     * Busca item específico dentro de uma lista.
     */
    Optional<ListItem> findByIdAndListId(UUID id, UUID listId);

    /**
     * Busca item com todos os relacionamentos carregados (evita LazyInitializationException).
     */
    @Query("SELECT li FROM list_items li " +
           "LEFT JOIN FETCH li.list " +
           "LEFT JOIN FETCH li.createdBy " +
           "WHERE li.id = :id")
    Optional<ListItem> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Conta quantos itens existem em uma lista.
     */
    Long countByListId(UUID listId);
}
