package br.com.leoferolive.nossalista.auth.dto;

import br.com.leoferolive.nossalista.auth.domain.AuthProvider;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user registration
 * IMPORTANT: Password field is NOT included for security reasons
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
