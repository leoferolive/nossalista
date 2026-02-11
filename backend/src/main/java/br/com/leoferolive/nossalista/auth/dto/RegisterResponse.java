package br.com.leoferolive.nossalista.auth.dto;

import br.com.leoferolive.nossalista.auth.domain.AuthProvider;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para registro de usuário
 * IMPORTANTE: Campo password NÃO é incluído por razões de segurança
 */
public record RegisterResponse(
    UUID id,
    String username,
    String email,
    String name,
    String avatarUrl,
    AuthProvider authProvider,
    LocalDateTime createdAt
) {
}
