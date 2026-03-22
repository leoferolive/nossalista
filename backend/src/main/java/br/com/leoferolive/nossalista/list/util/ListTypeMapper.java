package br.com.leoferolive.nossalista.list.util;

import br.com.leoferolive.nossalista.list.domain.ListTypeEntity;

import java.util.Map;

/**
 * Utilitário estático para mapear typeId para slug/nome quando a
 * {@link ListTypeEntity} não está carregada (LAZY).
 *
 * <p>Centraliza o mapeamento que antes estava duplicado em ListMapper,
 * ListJoinService e List.getType().
 */
public final class ListTypeMapper {

    private ListTypeMapper() {
        // utility class
    }

    private static final Map<Integer, String> SLUG_BY_ID = Map.of(
        1, "compras",
        2, "tarefas",
        3, "wishlist",
        4, "generica"
    );

    private static final Map<Integer, String> NAME_BY_ID = Map.of(
        1, "Compras",
        2, "Tarefas",
        3, "Wishlist",
        4, "Genérica"
    );

    /**
     * Retorna o slug para o typeId informado.
     *
     * @param typeId ID do tipo (1-4)
     * @return slug correspondente ou "unknown" se typeId não mapeado
     */
    public static String getSlug(Integer typeId) {
        return SLUG_BY_ID.getOrDefault(typeId, "unknown");
    }

    /**
     * Retorna o nome de exibição para o typeId informado.
     *
     * @param typeId ID do tipo (1-4)
     * @return nome correspondente ou "Unknown" se typeId não mapeado
     */
    public static String getName(Integer typeId) {
        return NAME_BY_ID.getOrDefault(typeId, "Unknown");
    }

    /**
     * Resolve o slug usando a entidade quando disponível, ou fallback pelo typeId.
     *
     * @param typeEntity entidade JPA (pode ser null se LAZY não carregada)
     * @param typeId     ID do tipo como fallback
     * @return slug do tipo
     */
    public static String resolveSlug(ListTypeEntity typeEntity, Integer typeId) {
        if (typeEntity != null) {
            return typeEntity.getSlug();
        }
        return getSlug(typeId);
    }

    /**
     * Resolve o nome usando a entidade quando disponível, ou fallback pelo typeId.
     *
     * @param typeEntity entidade JPA (pode ser null se LAZY não carregada)
     * @param typeId     ID do tipo como fallback
     * @return nome de exibição do tipo
     */
    public static String resolveName(ListTypeEntity typeEntity, Integer typeId) {
        if (typeEntity != null) {
            return typeEntity.getName();
        }
        return getName(typeId);
    }

    /**
     * Resolve o ID usando a entidade quando disponível, ou o typeId direto.
     *
     * @param typeEntity entidade JPA (pode ser null se LAZY não carregada)
     * @param typeId     ID do tipo como fallback
     * @return ID do tipo
     */
    public static Integer resolveId(ListTypeEntity typeEntity, Integer typeId) {
        if (typeEntity != null) {
            return typeEntity.getId();
        }
        return typeId;
    }
}
