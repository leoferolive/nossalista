package br.com.leoferolive.nossalista.list.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta para visualização de lista via convite (modo read-only)
 *
 * @param id              ID único da lista
 * @param name            Nome da lista
 * @param typeSlug        Slug do tipo da lista
 * @param typeName        Nome do tipo da lista
 * @param ownerUsername   Username do dono da lista
 * @param ownerName       Nome do dono da lista
 * @param ownerAvatarUrl  URL do avatar do dono (pode ser null)
 * @param items           Lista de itens (read-only view)
 * @param inviteCode      Código de convite usado
 * @param expiresAt       Data de expiração do convite
 * @param mode            Modo de acesso ("READ_ONLY")
 */
public record JoinListResponse(
    UUID id,
    String name,
    String typeSlug,
    String typeName,
    String ownerUsername,
    String ownerName,
    String ownerAvatarUrl,
    List<JoinListItemResponse> items,
    String inviteCode,
    LocalDateTime expiresAt,
    String mode
) {

    /**
     * DTO aninhado representando um item de lista no modo read-only
     *
     * @param id       ID único do item
     * @param name     Nome do item
     * @param checked  Indica se o item está marcado como concluído
     * @param quantity Quantidade (para listas de compras, pode ser null)
     * @param dueDate  Data de vencimento (para listas de tarefas, pode ser null)
     * @param url      URL (para listas de wishlist, pode ser null)
     * @param position Ordem/posição do item na lista
     */
    public record JoinListItemResponse(
        UUID id,
        String name,
        boolean checked,
        Integer quantity,
        LocalDateTime dueDate,
        String url,
        Integer position
    ) {
    }
}
