package br.com.leoferolive.nossalista.mcp.interceptor;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import io.micrometer.core.instrument.MeterRegistry;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração ponta-a-ponta (client MCP real) das métricas
 * Prometheus registradas por {@link McpToolMetrics} para as chamadas de tool.
 *
 * <p>Usa H2 dedicado ({@code mcp-metrics-it}) — mesma lição da Fase B
 * (D-020) sobre isolamento de banco em testes MCP de integração.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:mcp-metrics-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
class McpToolMetricsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @Autowired
    private PersonalAccessTokenService tokenService;

    @Autowired
    private MeterRegistry meterRegistry;

    private final List<McpSyncClient> openClients = new ArrayList<>();

    @AfterEach
    void closeClients() {
        openClients.forEach(McpSyncClient::close);
        openClients.clear();
    }

    private User newUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userService.createUser(
            "mcpmetricsuser" + suffix, "mcpmetricsuser" + suffix + "@example.com", "hashed",
            "MCP Metrics User " + suffix, AuthProvider.EMAIL);
    }

    private String issueToken(User user, TokenScope scope) {
        return tokenService.create(user.getId(), new CreatePersonalAccessTokenRequest("test-token", scope, null))
            .token();
    }

    private McpSyncClient connectedClient(String token) {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
            .builder("http://localhost:" + port)
            .endpoint("/mcp")
            .requestBuilder(HttpRequest.newBuilder().header("Authorization", "Bearer " + token))
            .build();
        McpSyncClient client = McpClient.sync(transport).build();
        client.initialize();
        openClients.add(client);
        return client;
    }

    private void call(McpSyncClient client, String toolName, Map<String, Object> arguments) {
        client.callTool(CallToolRequest.builder(toolName).arguments(arguments).build());
    }

    private double counterValue(String toolName, String outcome) {
        return meterRegistry.get(McpToolMetrics.CALLS_METRIC)
            .tag(McpToolMetrics.TOOL_TAG, toolName)
            .tag(McpToolMetrics.OUTCOME_TAG, outcome)
            .counter()
            .count();
    }

    private long timerCount(String toolName) {
        return meterRegistry.get(McpToolMetrics.DURATION_METRIC)
            .tag(McpToolMetrics.TOOL_TAG, toolName)
            .timer()
            .count();
    }

    @Test
    @DisplayName("chamada bem-sucedida incrementa counter outcome=success e o timer da tool")
    void successfulCallRecordsSuccessOutcomeAndDuration() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user, TokenScope.READ_WRITE));

        call(client, "list_my_lists", Map.of());

        assertThat(counterValue("list_my_lists", "success")).isGreaterThanOrEqualTo(1.0);
        assertThat(timerCount("list_my_lists")).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("erro de negócio (lista não encontrada) incrementa counter outcome=business_error")
    void businessErrorRecordsBusinessErrorOutcome() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user, TokenScope.READ_WRITE));

        call(client, "get_list", Map.of("listId", UUID.randomUUID().toString()));

        assertThat(counterValue("get_list", "business_error")).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("PAT read-only chamando tool de mutação incrementa counter outcome=denied")
    void scopeDenialRecordsDeniedOutcome() {
        User user = newUser();
        McpSyncClient readClient = connectedClient(issueToken(user, TokenScope.READ));

        call(readClient, "create_list", Map.of("name", "X", "type", "SHOPPING"));

        assertThat(counterValue("create_list", "denied")).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("as métricas mcp_tool_* ficam visíveis em /actuator/prometheus")
    void metricsAreExposedOnPrometheusEndpoint() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user, TokenScope.READ_WRITE));
        call(client, "list_my_lists", Map.of());

        String body = RestClient.create()
            .get()
            .uri("http://localhost:" + port + "/actuator/prometheus")
            .retrieve()
            .body(String.class);

        assertThat(body).contains(McpToolMetrics.CALLS_METRIC, McpToolMetrics.DURATION_METRIC);
    }
}
