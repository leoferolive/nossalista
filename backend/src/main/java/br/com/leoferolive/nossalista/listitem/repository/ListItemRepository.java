package br.com.leoferolive.nossalista.listitem.repository;

import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /**
     * Retorna o valor máximo de position em uma lista.
     * Retorna -1 se a lista não tiver itens (próximo será 0).
     *
     * @param listId ID da lista
     * @return max position ou -1 se lista vazia
     */
    @Query("SELECT COALESCE(MAX(li.position), -1) FROM list_items li WHERE li.list.id = :listId")
    Integer findMaxPositionByListId(@Param("listId") UUID listId);

    /**
     * Busca todos os itens com position maior que o valor especificado.
     * Usado para reordenação após deletar um item.
     *
     * @param listId ID da lista
     * @param position Position de referência
     * @return Lista de itens com position > referência, ordenados por position ASC
     */
    List<ListItem> findByListIdAndPositionGreaterThanOrderByPositionAsc(UUID listId, Integer position);

    /**
     * Conta itens agrupados por lista em uma única query (evita N+1).
     *
     * @param listIds coleção de IDs de listas
     * @return lista de Object[] onde [0] = UUID (listId), [1] = Long (count)
     */
    @Query("SELECT li.list.id, COUNT(li) FROM list_items li WHERE li.list.id IN :listIds GROUP BY li.list.id")
    List<Object[]> countByListIds(@Param("listIds") Collection<UUID> listIds);
}
