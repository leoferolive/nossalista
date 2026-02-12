package br.com.leoferolive.nossalista.list.repository;

import br.com.leoferolive.nossalista.list.domain.ListTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para operações da entidade ListTypeEntity
 */
@Repository
public interface ListTypeRepository extends JpaRepository<ListTypeEntity, Integer> {

    /**
     * Busca um tipo de lista pelo seu slug.
     *
     * @param slug o slug para buscar (compras, tarefas, wishlist, generica)
     * @return Optional contendo o tipo se encontrado
     */
    Optional<ListTypeEntity> findBySlug(String slug);
}
