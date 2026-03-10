package br.com.leoferolive.nossalista.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para perfil completo do usuário
 * Contém todos os dados do próprio usuário autenticado (sem password)
 */
public record UserProfileResponse(
    UUID id,
    String username,
    String email,
    String name,
    String avatarUrl,
    String authProvider,
    LocalDateTime onboardingCompletedAt,
    LocalDateTime createdAt
) {
}
