package br.com.leoferolive.nossalista.apitoken.dto;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metadados de um Personal Access Token, sem o valor em claro nem o hash —
 * usado na listagem ({@code GET /api/users/me/tokens}).
 */
public record PersonalAccessTokenResponse(
    UUID id,
    String name,
    String prefix,
    TokenScope scope,
    LocalDateTime expiresAt,
    LocalDateTime lastUsedAt,
    LocalDateTime createdAt
) {
}
