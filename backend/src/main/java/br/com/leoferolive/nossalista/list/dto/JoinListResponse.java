package br.com.leoferolive.nossalista.list.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta para visualização de lista via convite (modo read-only)
 *
 * @param id               ID único da lista
 * @param name             Nome da lista
 * @param type_slug        Slug do tipo da lista (snake_case)
 * @param type_name        Nome do tipo da lista (snake_case)
 * @param owner_username   Username do dono da lista (snake_case)
 * @param owner_name       Nome do dono da lista (snake_case)
 * @param owner_avatar_url URL do avatar do dono (snake_case, pode ser null)
 * @param items            Lista de itens (read-only view)
 * @param invite_code      Código de convite usado (snake_case)
 * @param expires_at       Data de expiração do convite (snake_case)
 * @param mode             Modo de acesso ("READ_ONLY")
 */
public record JoinListResponse(
    UUID id,
    String name,
    String type_slug,
    String type_name,
    String owner_username,
    String owner_name,
    String owner_avatar_url,
    List<JoinListItemResponse> items,
    String invite_code,
    LocalDateTime expires_at,
    String mode
) {

    /**
     * DTO aninhado representando um item de lista no modo read-only
     *
     * @param id       ID único do item
     * @param name     Nome do item
     * @param checked  Indica se o item está marcado como concluído
     * @param quantity Quantidade (para listas de compras, pode ser null)
     * @param due_date Data de vencimento (para listas de tarefas, pode ser null)
     * @param url      URL (para listas de wishlist, pode ser null)
     * @param position Ordem/posição do item na lista
     */
    public record JoinListItemResponse(
        UUID id,
        String name,
        boolean checked,
        Integer quantity,
        LocalDateTime due_date,
        String url,
        Integer position
    ) {
    }
}
