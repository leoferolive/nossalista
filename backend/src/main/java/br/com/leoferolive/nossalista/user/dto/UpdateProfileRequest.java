package br.com.leoferolive.nossalista.user.dto;

/**
 * DTO de requisição para atualização de perfil
 * Apenas name e avatarUrl podem ser alterados (opcionais)
 * username e email são readonly - não incluídos neste DTO
 */
public record UpdateProfileRequest(
    String name,
    String avatarUrl
) {
}
