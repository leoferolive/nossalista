package br.com.leoferolive.nossalista.user.dto;

/**
 * DTO de resposta para busca de usuários
 * Contém apenas informações públicas (sem email ou password)
 */
public record UserSearchResponse(
    String username,
    String name,
    String avatarUrl
) {
}
