package br.com.leoferolive.nossalista.mcp.interceptor;

import br.com.leoferolive.nossalista.config.RateLimiterService;
import br.com.leoferolive.nossalista.mcp.security.McpRateLimitExceededException;
import br.com.leoferolive.nossalista.mcp.security.McpSecurityContext;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Aplica um teto de mutações por usuário/minuto às tools MCP que alteram
 * dados, via {@link RateLimiterService} (chave por {@code userId}, não por
 * IP — diferente do rate limit de tentativas inválidas de PAT em
 * {@code PersonalAccessTokenAuthenticationFilter}).
 *
 * <p>Chamado explicitamente por uma única linha logo após
 * {@link McpSecurityContext#requireWriteAccess()} em cada uma das 9 tools de
 * mutação (mesmo padrão de guard-clause já usado nelas) — não via Spring AOP:
 * criar um proxy CGLIB para as classes de tool ({@code @McpTool} exige que o
 * SDK do Spring AI MCP invoque o método via reflection sobre a instância
 * exata registrada no scanner de anotações) quebra o dispatch assíncrono do
 * transporte Streamable HTTP (a segunda passagem pelos filtros de segurança,
 * no completamento assíncrono, deixa de autenticar a requisição). Ver
 * {@code docs/DECISIONS.md} D-023.</p>
 */
@Component
public class McpMutationRateLimiter {

    /** Mutações permitidas por usuário, por janela — ver docs/DECISIONS.md D-023. */
    static final int MAX_MUTATIONS_PER_WINDOW = 60;

    static final Duration WINDOW = Duration.ofMinutes(1);

    private static final String KEY_PREFIX = "mcp-mutation:user:";

    private final RateLimiterService rateLimiterService;
    private final McpSecurityContext security;

    public McpMutationRateLimiter(RateLimiterService rateLimiterService, McpSecurityContext security) {
        this.rateLimiterService = rateLimiterService;
        this.security = security;
    }

    /**
     * @throws McpRateLimitExceededException se o usuário autenticado já excedeu
     *     {@link #MAX_MUTATIONS_PER_WINDOW} mutações na janela corrente
     */
    public void enforce() {
        User user = security.currentUser();
        String key = KEY_PREFIX + user.getId();
        if (!rateLimiterService.isAllowed(key, MAX_MUTATIONS_PER_WINDOW, WINDOW)) {
            throw new McpRateLimitExceededException(
                "Rate limit exceeded: too many mutating tool calls (max " + MAX_MUTATIONS_PER_WINDOW
                    + " per minute). Wait a moment before retrying.");
        }
    }
}
