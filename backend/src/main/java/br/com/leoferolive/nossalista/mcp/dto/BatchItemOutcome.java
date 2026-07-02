package br.com.leoferolive.nossalista.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Resultado individual de um item dentro de uma operação em lote
 * ({@code add_items}, {@code set_items_checked}, {@code remove_items}).
 *
 * @param itemId  id do item (nulo se a criação falhou antes de gerar um id)
 * @param name    nome do item, quando disponível
 * @param success se a operação teve sucesso para este item
 * @param error   mensagem acionável do motivo da falha, ou {@code null} em caso de sucesso
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchItemOutcome(
    @Nullable String itemId,
    @Nullable String name,
    boolean success,
    @Nullable String error
) {
}
