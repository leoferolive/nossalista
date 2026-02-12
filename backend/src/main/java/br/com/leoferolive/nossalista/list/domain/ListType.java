package br.com.leoferolive.nossalista.list.domain;

/**
 * Enum representando os tipos de lista disponíveis no NossaLista.
 * Sincronizado com a tabela list_types no database.
 *
 * Tipos pré-definidos:
 * - SHOPPING: Listas de compras (com campo quantidade)
 * - TASK: Listas de tarefas (com campo due_date)
 * - WISHLIST: Listas de desejos (com campo url)
 * - GENERIC: Listas genéricas (sem campos extras)
 */
public enum ListType {
    SHOPPING("compras"),
    TASK("tarefas"),
    WISHLIST("wishlist"),
    GENERIC("generica");

    private final String slug;

    ListType(String slug) {
        this.slug = slug;
    }

    /**
     * Retorna o slug do tipo de lista (lowercase).
     *
     * @return o slug correspondente ao tipo
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Converte um slug para o enum ListType correspondente.
     *
     * @param slug o slug para converter
     * @return o ListType correspondente
     * @throws IllegalArgumentException se o slug for desconhecido
     */
    public static ListType fromSlug(String slug) {
        for (ListType type : values()) {
            if (type.slug.equals(slug)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown list type slug: " + slug);
    }
}
