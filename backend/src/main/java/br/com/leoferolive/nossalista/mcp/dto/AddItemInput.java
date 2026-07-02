package br.com.leoferolive.nossalista.mcp.dto;

import org.jspecify.annotations.Nullable;

/**
 * Um item de entrada da tool {@code add_items}.
 *
 * <p>Os campos opcionais são anotados {@code @Nullable} (jspecify) para que o
 * gerador de JSON Schema do Spring AI não os marque como {@code required} —
 * sem isso, todo componente de record é tratado como obrigatório por padrão.</p>
 *
 * @param name     nome do item (obrigatório)
 * @param quantity quantidade — apenas listas SHOPPING
 * @param dueDate  prazo em ISO-8601 — apenas listas TASK
 * @param url      URL — apenas listas WISHLIST
 */
public record AddItemInput(
    String name,
    @Nullable Integer quantity,
    @Nullable String dueDate,
    @Nullable String url
) {
}
