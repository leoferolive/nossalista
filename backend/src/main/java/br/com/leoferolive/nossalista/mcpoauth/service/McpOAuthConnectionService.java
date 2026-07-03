package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties.ClientDefinition;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRefreshToken;
import br.com.leoferolive.nossalista.mcpoauth.dto.OAuthConnectionSummary;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthRefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lista e revoga conexões OAuth do usuário (assistentes conectados via
 * claude.ai/Claude Code) para a tela "Conexões" do SPA.
 */
@Service
public class McpOAuthConnectionService {

    private final McpOAuthRefreshTokenRepository refreshTokenRepository;
    private final McpOAuthClientRegistry clientRegistry;

    public McpOAuthConnectionService(
        McpOAuthRefreshTokenRepository refreshTokenRepository, McpOAuthClientRegistry clientRegistry
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clientRegistry = clientRegistry;
    }

    /**
     * Uma conexão ativa por cliente ({@code clientId}), representada pela
     * família de refresh token mais recente ainda válida.
     */
    @Transactional(readOnly = true)
    public List<OAuthConnectionSummary> list(UUID userId) {
        List<McpOAuthRefreshToken> active = refreshTokenRepository.findActiveByUserId(userId, LocalDateTime.now());

        Map<String, McpOAuthRefreshToken> mostRecentByClient = new LinkedHashMap<>();
        for (McpOAuthRefreshToken token : active) {
            mostRecentByClient.putIfAbsent(token.getClientId(), token);
        }

        return mostRecentByClient.values().stream()
            .map(this::toSummary)
            .toList();
    }

    /**
     * Desconecta um cliente: revoga TODAS as famílias de refresh token ativas
     * do usuário para esse {@code clientId}, encerrando o acesso do assistente.
     */
    @Transactional
    public void disconnect(UUID userId, String clientId) {
        refreshTokenRepository.revokeAllForUserAndClient(userId, clientId, LocalDateTime.now());
    }

    private OAuthConnectionSummary toSummary(McpOAuthRefreshToken token) {
        String clientName = clientRegistry.find(token.getClientId())
            .map(ClientDefinition::getName)
            .orElse(token.getClientId());
        return new OAuthConnectionSummary(
            token.getClientId(), clientName, token.getScope(), token.getCreatedAt(), token.getLastUsedAt());
    }
}
