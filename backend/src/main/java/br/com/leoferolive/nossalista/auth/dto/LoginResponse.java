package br.com.leoferolive.nossalista.auth.dto;

import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para login bem-sucedido
 *
 * @param id           ID único do usuário
 * @param username     Nome de usuário único
 * @param email        Email do usuário
 * @param name         Nome completo do usuário
 * @param avatarUrl    URL do avatar (pode ser null)
 * @param onboardingCompletedAt Data/hora de conclusão do onboarding (pode ser null)
 * @param authProvider Provedor de autenticação (EMAIL, GOOGLE)
 * @param createdAt    Data de criação da conta
 * @param token        JWT token para autenticação
 * @param expiresAt    Data/hora de expiração do token
 */
public record LoginResponse(
    UUID id,
    String username,
    String email,
    String name,
    String avatarUrl,
    LocalDateTime onboardingCompletedAt,
    AuthProvider authProvider,
    LocalDateTime createdAt,
    String token,
    LocalDateTime expiresAt
) {
}
