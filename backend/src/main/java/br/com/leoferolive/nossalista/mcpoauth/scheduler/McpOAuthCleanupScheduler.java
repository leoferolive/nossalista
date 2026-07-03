package br.com.leoferolive.nossalista.mcpoauth.scheduler;

import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthCodeRepository;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthRefreshTokenRepository;
import br.com.leoferolive.nossalista.mcpoauth.repository.PendingAuthorizationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Varredura de limpeza (a cada 5min, mesmo padrão de
 * {@code RateLimiterCleanupScheduler}) de pedidos de autorização pendentes,
 * authorization codes e refresh tokens OAuth expirados — evita acúmulo de
 * linhas órfãs nas tabelas {@code mcp_oauth_*}.
 *
 * <p>Mantido como scheduler PRÓPRIO (em vez de estender
 * {@code RateLimiterCleanupScheduler}) para não conflitar com trabalho em
 * paralelo nesse arquivo (Fase D mexe em rate limiting).</p>
 */
@Component
public class McpOAuthCleanupScheduler {

    private final PendingAuthorizationRepository pendingAuthorizationRepository;
    private final McpOAuthCodeRepository codeRepository;
    private final McpOAuthRefreshTokenRepository refreshTokenRepository;

    public McpOAuthCleanupScheduler(
        PendingAuthorizationRepository pendingAuthorizationRepository,
        McpOAuthCodeRepository codeRepository,
        McpOAuthRefreshTokenRepository refreshTokenRepository
    ) {
        this.pendingAuthorizationRepository = pendingAuthorizationRepository;
        this.codeRepository = codeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        pendingAuthorizationRepository.deleteByExpiresAtBefore(now);
        codeRepository.deleteByExpiresAtBefore(now);
        refreshTokenRepository.deleteByExpiresAtBefore(now);
    }
}
