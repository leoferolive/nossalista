package br.com.leoferolive.nossalista.mcp.interceptor;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.config.RateLimiterService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração ponta-a-ponta (client MCP real, Streamable HTTP) do
 * teto de mutações por usuário/minuto de {@link McpMutationRateLimiter}.
 *
 * <p>Usa H2 dedicado ({@code mcp-rl-it}), isolado do {@code testdb}
 * compartilhado e do {@code mcp-it} de {@code McpServerIntegrationTest} —
 * mesma lição da Fase B (D-020): testes que criam muitos recursos poluem
 * bancos compartilhados e quebram asserts de contagem absoluta em outras
 * classes quando o Surefire reordena a execução.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:mcp-rl-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
class McpMutationRateLimitIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @Autowired
    private PersonalAccessTokenService tokenService;

    @Autowired
    private RateLimiterService rateLimiterService;

    private final List<McpSyncClient> openClients = new ArrayList<>();

    @BeforeEach
    void resetRateLimiter() {
        rateLimiterService.reset();
    }

    @AfterEach
    void closeClients() {
        openClients.forEach(McpSyncClient::close);
        openClients.clear();
        rateLimiterService.reset();
    }

    private User newUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userService.createUser(
            "mcprluser" + suffix, "mcprluser" + suffix + "@example.com", "hashed", "MCP RL User " + suffix,
            AuthProvider.EMAIL);
    }

    private String issueToken(User user) {
        return tokenService.create(
            user.getId(), new CreatePersonalAccessTokenRequest("test-token", TokenScope.READ_WRITE, null)
        ).token();
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

    private CallToolResult call(McpSyncClient client, String toolName, Map<String, Object> arguments) {
        return client.callTool(CallToolRequest.builder(toolName).arguments(arguments).build());
    }

    private String textOf(CallToolResult result) {
        if (result.content() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (McpSchema.Content content : result.content()) {
            if (content instanceof TextContent textContent) {
                builder.append(textContent.text());
            }
        }
        return builder.toString();
    }

    private void assertRateLimited(CallToolResult result) {
        assertThat(result.isError()).isEqualTo(Boolean.TRUE);
        assertThat(textOf(result)).containsIgnoringCase("rate limit exceeded");
    }

    private String createShoppingList(McpSyncClient client) {
        CallToolResult created = call(client, "create_list",
            Map.of("name", "Lista RL " + UUID.randomUUID(), "type", "SHOPPING"));
        assertThat(created.isError()).as("create_list não deveria falhar: %s", textOf(created)).isNotEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> structured = (Map<String, Object>) created.structuredContent();
        return (String) structured.get("id");
    }

    /**
     * Consome {@code count} unidades do teto do usuário chamando
     * {@code rename_list} repetidamente — mutação idempotente que não
     * consome nenhum recurso próprio (a lista continua existindo e
     * renomeável indefinidamente), ideal para "encher" o teto sem
     * interferir no cenário sob teste.
     */
    private void burnMutationBudget(McpSyncClient client, String listId, int count) {
        for (int i = 0; i < count; i++) {
            call(client, "rename_list", Map.of("listId", listId, "name", "Renomeada " + i));
        }
    }

    @Test
    @DisplayName("mutações dentro do teto (60/min) são sempre permitidas")
    void mutationsWithinLimitAreAllowed() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user));
        String listId = createShoppingList(client);

        // já usou 1 (create_list); mais (MAX - 2) para ficar bem dentro do teto
        burnMutationBudget(client, listId, McpMutationRateLimiter.MAX_MUTATIONS_PER_WINDOW - 2);

        CallToolResult result = call(client, "rename_list", Map.of("listId", listId, "name", "Ainda ok"));
        assertThat(result.isError()).as("não deveria ter atingido o teto ainda: %s", textOf(result))
            .isNotEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("a mutação que excede o teto (60/min) é bloqueada com erro acionável")
    void mutationExceedingLimitIsBlocked() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user));
        String listId = createShoppingList(client);

        // create_list já usou 1; completa o teto exato
        burnMutationBudget(client, listId, McpMutationRateLimiter.MAX_MUTATIONS_PER_WINDOW - 1);

        assertRateLimited(call(client, "rename_list", Map.of("listId", listId, "name", "Excedeu")));
    }

    @Test
    @DisplayName("tools de leitura nunca contam para o teto de mutação")
    void readToolsNeverCountTowardsMutationLimit() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user));
        String listId = createShoppingList(client);

        for (int i = 0; i < McpMutationRateLimiter.MAX_MUTATIONS_PER_WINDOW * 2; i++) {
            call(client, "get_list", Map.of("listId", listId));
        }

        CallToolResult result = call(client, "rename_list", Map.of("listId", listId, "name", "Depois de ler muito"));
        assertThat(result.isError()).as("leituras não deveriam consumir o teto: %s", textOf(result))
            .isNotEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("o teto é por usuário: outro usuário não é afetado pelo consumo do primeiro")
    void limitIsPerUserNotGlobal() {
        User userA = newUser();
        User userB = newUser();
        McpSyncClient clientA = connectedClient(issueToken(userA));
        McpSyncClient clientB = connectedClient(issueToken(userB));
        String listAId = createShoppingList(clientA);
        String listBId = createShoppingList(clientB);

        burnMutationBudget(clientA, listAId, McpMutationRateLimiter.MAX_MUTATIONS_PER_WINDOW - 1);
        assertRateLimited(call(clientA, "rename_list", Map.of("listId", listAId, "name", "A excedeu")));

        CallToolResult resultB = call(clientB, "rename_list", Map.of("listId", listBId, "name", "B intacto"));
        assertThat(resultB.isError()).as("usuário B não deveria ser afetado: %s", textOf(resultB))
            .isNotEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("após reset da janela, o usuário volta a mutar normalmente")
    void windowResetAllowsMutationsAgain() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user));
        String listId = createShoppingList(client);

        burnMutationBudget(client, listId, McpMutationRateLimiter.MAX_MUTATIONS_PER_WINDOW - 1);
        assertRateLimited(call(client, "rename_list", Map.of("listId", listId, "name", "Bloqueado")));

        rateLimiterService.reset();

        CallToolResult result = call(client, "rename_list", Map.of("listId", listId, "name", "Liberado de novo"));
        assertThat(result.isError()).as("após reset da janela deveria voltar a permitir: %s", textOf(result))
            .isNotEqualTo(Boolean.TRUE);
    }

    /**
     * Uma tupla por tool de mutação, com argumentos dummy — o aspecto
     * intercepta e bloqueia ANTES de qualquer parsing/lookup real, então
     * IDs fictícios bastam para provar o bloqueio (mesmo raciocínio de
     * {@code McpServerIntegrationTest.allMutationToolCalls}).
     */
    private static Stream<Arguments> allMutationToolCalls() {
        String dummyId = "00000000-0000-0000-0000-000000000000";
        return Stream.of(
            Arguments.of("create_list", Map.of("name", "X", "type", "SHOPPING")),
            Arguments.of("rename_list", Map.of("listId", dummyId, "name", "Y")),
            Arguments.of("delete_list", Map.of("listId", dummyId)),
            Arguments.of("add_items", Map.of("listId", dummyId, "items", List.of(Map.of("name", "X")))),
            Arguments.of("update_item", Map.of("listId", dummyId, "itemId", dummyId, "name", "Y")),
            Arguments.of("set_items_checked",
                Map.of("listId", dummyId, "itemIds", List.of(dummyId), "checked", true)),
            Arguments.of("remove_items", Map.of("listId", dummyId, "itemIds", List.of(dummyId))),
            Arguments.of("share_list", Map.of("listId", dummyId, "mode", "link")),
            Arguments.of("remove_member", Map.of("listId", dummyId, "userId", dummyId))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allMutationToolCalls")
    @DisplayName("todas as 9 tools de mutação são bloqueadas quando o usuário já excedeu o teto")
    void everyMutationToolIsBlockedOnceLimitExceeded(String toolName, Map<String, Object> arguments) {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user));
        String listId = createShoppingList(client);

        burnMutationBudget(client, listId, McpMutationRateLimiter.MAX_MUTATIONS_PER_WINDOW - 1);

        assertRateLimited(call(client, toolName, arguments));
    }
}
