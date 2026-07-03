package br.com.leoferolive.nossalista.mcpoauth.dto;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;

import java.time.LocalDateTime;

/**
 * Resumo de uma conexão OAuth ativa (família de refresh token viva) exibido na
 * tela "Conexões" — um por cliente ({@code clientId}) conectado pelo usuário.
 */
public record OAuthConnectionSummary(
    String clientId,
    String clientName,
    TokenScope scope,
    LocalDateTime createdAt,
    LocalDateTime lastUsedAt
) {
}
