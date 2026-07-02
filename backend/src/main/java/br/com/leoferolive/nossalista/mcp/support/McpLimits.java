package br.com.leoferolive.nossalista.mcp.support;

import br.com.leoferolive.nossalista.common.exception.InvalidInputException;

/**
 * Tetos de tamanho de lote e de página para as tools MCP.
 *
 * <p>Sem um teto, um token válido (ou um modelo induzido por prompt
 * injection) poderia mandar um lote/página gigante numa única chamada —
 * {@code add_items}/{@code set_items_checked}/{@code remove_items} sem
 * limite de itens, ou {@code get_list}/{@code get_list_activity} sem limite
 * de página, executam N inserts/toggles/deletes/broadcasts ou uma consulta
 * pesada numa única requisição. Num backend de réplica única isso pode
 * travar o pod para todos, e nenhum rate limit hoje cobre requisições
 * autenticadas válidas (só tentativas de autenticação inválida). Achado do
 * review sênior do PR da Fase B — ver {@code docs/DECISIONS.md} D-020.</p>
 */
public final class McpLimits {

    /** Máximo de itens por chamada em lote (add_items/set_items_checked/remove_items). */
    public static final int MAX_BATCH_SIZE = 200;

    /** Máximo de itens/entradas por página (get_list/get_list_activity). */
    public static final int MAX_PAGE_SIZE = 500;

    private McpLimits() {
    }

    /**
     * @throws InvalidInputException se {@code size} exceder {@link #MAX_BATCH_SIZE}
     */
    public static void requireBatchSizeWithinLimit(int size, String fieldName) {
        if (size > MAX_BATCH_SIZE) {
            throw new InvalidInputException(
                fieldName + " excede o máximo de " + MAX_BATCH_SIZE + " itens por chamada (recebido: " + size + ")");
        }
    }

    /**
     * Resolve o tamanho de página efetivo: {@code defaultSize} se {@code requested}
     * for nulo ou não-positivo, o próprio {@code requested} caso contrário.
     *
     * @throws InvalidInputException se {@code requested} exceder {@link #MAX_PAGE_SIZE}
     */
    public static int requirePageSizeWithinLimit(Integer requested, int defaultSize, String fieldName) {
        if (requested == null || requested <= 0) {
            return defaultSize;
        }
        if (requested > MAX_PAGE_SIZE) {
            throw new InvalidInputException(
                fieldName + " excede o máximo de " + MAX_PAGE_SIZE + " (recebido: " + requested + ")");
        }
        return requested;
    }
}
