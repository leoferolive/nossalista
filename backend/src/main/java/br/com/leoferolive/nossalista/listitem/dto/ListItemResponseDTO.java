package br.com.leoferolive.nossalista.listitem.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para item de lista
 *
 * @param id          ID único do item
 * @param name        Nome do item
 * @param checked     Status de concluído
 * @param quantity    Quantidade (para tipo Compras)
 * @param dueDate     Data de vencimento (para tipo Tarefas)
 * @param url         URL do produto (para tipo Wishlist)
 * @param position    Posição na lista (ordenação)
 * @param createdBy   Informações do criador (id, username, avatar)
 * @param createdAt   Data de criação
 * @param updatedAt   Data da última atualização
 */
public record ListItemResponseDTO(
    UUID id,
    String name,
    boolean checked,
    Integer quantity,
    LocalDateTime dueDate,
    String url,
    Integer position,
    CreatorResponse createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    /**
     * DTO aninhado representando o criador do item
     *
     * @param id       ID do usuário
     * @param username Nome de usuário
     * @param name     Nome completo
     * @param avatarUrl URL do avatar
     */
    public record CreatorResponse(
        UUID id,
        String username,
        String name,
        String avatarUrl
    ) {
    }
}
