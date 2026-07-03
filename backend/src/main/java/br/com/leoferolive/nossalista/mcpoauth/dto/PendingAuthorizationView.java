package br.com.leoferolive.nossalista.mcpoauth.dto;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;

import java.util.UUID;

/**
 * Dados exibidos na tela de consentimento da SPA para um pedido de autorização
 * pendente — nunca inclui o {@code code_challenge} (irrelevante para o usuário
 * e não deve circular além do necessário).
 */
public record PendingAuthorizationView(
    UUID requestId,
    String clientId,
    String clientName,
    TokenScope scope,
    String redirectUriHost
) {
}
