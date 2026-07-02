package br.com.leoferolive.nossalista.apitoken.dto;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta da criação de um Personal Access Token — a ÚNICA vez em que o
 * valor em claro ({@link #token()}) é exposto pela API. O backend nunca o
 * persiste nem consegue recuperá-lo depois (apenas o hash é guardado).
 */
public record PersonalAccessTokenCreatedResponse(
    UUID id,
    String name,
    String token,
    String prefix,
    TokenScope scope,
    LocalDateTime expiresAt,
    LocalDateTime createdAt
) {
}
