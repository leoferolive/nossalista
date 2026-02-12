package br.com.leoferolive.nossalista.list.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para lista criada
 *
 * @param id         ID único da lista
 * @param name       Nome da lista
 * @param type       Tipo da lista (com id, nome e slug)
 * @param owner      Dono da lista (com id, username e avatarUrl)
 * @param inviteCode Código de convite para compartilhar
 * @param createdAt  Data de criação
 * @param updatedAt  Data da última atualização
 */
public record ListResponse(
    UUID id,
    String name,
    TypeResponse type,
    OwnerResponse owner,
    String inviteCode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    /**
     * DTO aninhado representando o tipo da lista
     *
     * @param id   ID do tipo (1-4)
     * @param name Nome do tipo (Compras, Tarefas, Wishlist, Genérica)
     * @param slug Slug do tipo (compras, tarefas, wishlist, generica)
     */
    public record TypeResponse(
        Integer id,
        String name,
        String slug
    ) {
    }

    /**
     * DTO aninhado representando o dono da lista
     *
     * @param id       ID do usuário
     * @param username Nome de usuário único
     * @param avatarUrl URL do avatar (pode ser null)
     */
    public record OwnerResponse(
        UUID id,
        String username,
        String avatarUrl
    ) {
    }
}
