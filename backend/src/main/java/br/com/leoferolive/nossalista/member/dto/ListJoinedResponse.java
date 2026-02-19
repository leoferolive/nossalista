package br.com.leoferolive.nossalista.member.dto;

import br.com.leoferolive.nossalista.list.dto.ListResponse;

import java.util.UUID;

/**
 * DTO de resposta para quando um usuário entra em uma lista via convite.
 * <p>
 * Retornado pelo endpoint POST /api/lists/join/{inviteCode} quando:
 * <ul>
 *   <li>201 Created: Usuário acabou de se tornar membro da lista</li>
 *   <li>200 OK: Usuário já era membro ou é o dono da lista</li>
 * </ul>
 *
 * @param id          ID único da lista
 * @param name        Nome da lista
 * @param type_slug   Slug do tipo da lista (snake_case)
 * @param type_name   Nome do tipo da lista (snake_case)
 * @param role        Papel do usuário na lista: "OWNER" ou "MEMBER"
 * @param message     Mensagem amigável para o usuário (ex: "Bem-vindo à lista X!")
 * @param created     true quando novo membro criado (201 Created), false quando já existia (200 OK)
 * @param list        Dados completos da lista (mesmo formato do GET /api/lists/{id})
 */
public record ListJoinedResponse(
    UUID id,
    String name,
    String type_slug,
    String type_name,
    String role,
    String message,
    boolean created,
    ListResponse list
) {
}
