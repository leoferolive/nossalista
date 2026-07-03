package br.com.leoferolive.nossalista.mcp.interceptor;

import br.com.leoferolive.nossalista.mcp.security.McpAuthenticationException;
import br.com.leoferolive.nossalista.mcp.security.McpRateLimitExceededException;
import br.com.leoferolive.nossalista.mcp.security.McpScopeException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Wrapper comum de observabilidade para as 13 tools MCP, via Micrometer:
 * counter {@code mcp_tool_calls_total{tool, outcome}} (outcome:
 * {@code success}|{@code business_error}|{@code denied}) e timer
 * {@code mcp_tool_duration_seconds{tool}}.
 *
 * <p>Cada tool chama {@link #record(String, Supplier)} envolvendo seu corpo
 * original — uma linha por tool, sem duplicar a lógica de métricas 13 vezes.
 * Não é feito via Spring AOP: ver {@code McpMutationRateLimiter} para o porquê
 * de evitar proxies CGLIB nas classes de tool (quebra o dispatch assíncrono
 * do SDK Spring AI MCP). Ver {@code docs/DECISIONS.md} D-022.</p>
 *
 * <p>{@code denied} cobre autenticação/escopo/rate-limit (o chamador não
 * chegou a executar a operação); {@code business_error} cobre qualquer outra
 * falha que o SDK converte em resultado de tool {@code isError: true}
 * (validação, não encontrado, sem permissão sobre o recurso etc.).</p>
 */
@Component
public class McpToolMetrics {

    static final String CALLS_METRIC = "mcp_tool_calls_total";
    static final String DURATION_METRIC = "mcp_tool_duration_seconds";
    static final String TOOL_TAG = "tool";
    static final String OUTCOME_TAG = "outcome";

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_DENIED = "denied";
    private static final String OUTCOME_BUSINESS_ERROR = "business_error";

    private final MeterRegistry registry;

    public McpToolMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Executa {@code call}, registrando duração e contagem por outcome.
     * Repropaga qualquer exceção lançada por {@code call} inalterada — quem
     * decide como ela vira {@code isError: true} continua sendo o SDK MCP.
     */
    public <T> T record(String toolName, Supplier<T> call) {
        Timer.Sample sample = Timer.start(registry);
        String outcome = OUTCOME_SUCCESS;
        try {
            return call.get();
        } catch (RuntimeException ex) {
            outcome = classify(ex);
            throw ex;
        } finally {
            // publishPercentileHistogram(): expõe buckets _bucket no /actuator/prometheus,
            // necessários para o painel de p95 do dashboard Grafana calcular
            // histogram_quantile(0.95, ...) — sem isso só count/sum/max ficam disponíveis.
            sample.stop(Timer.builder(DURATION_METRIC)
                .tag(TOOL_TAG, toolName)
                .publishPercentileHistogram()
                .register(registry));
            Counter.builder(CALLS_METRIC)
                .tag(TOOL_TAG, toolName)
                .tag(OUTCOME_TAG, outcome)
                .register(registry)
                .increment();
        }
    }

    private String classify(RuntimeException ex) {
        if (ex instanceof McpAuthenticationException
            || ex instanceof McpScopeException
            || ex instanceof McpRateLimitExceededException) {
            return OUTCOME_DENIED;
        }
        return OUTCOME_BUSINESS_ERROR;
    }
}
