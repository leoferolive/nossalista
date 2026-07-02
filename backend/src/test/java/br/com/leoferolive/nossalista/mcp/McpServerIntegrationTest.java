package br.com.leoferolive.nossalista.mcp;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import br.com.leoferolive.nossalista.websocket.WebSocketEventPublisher;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClient;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Testes de integração ponta-a-ponta do servidor MCP usando o client oficial
 * do SDK Java (Streamable HTTP) contra a aplicação real (porta aleatória).
 *
 * <p>Cada teste cria seus próprios usuários/listas com nomes únicos, porque o
 * contexto Spring (e o H2 em memória) é compartilhado entre os métodos desta
 * classe — chamadas via HTTP real rodam em outra thread, então o rollback de
 * {@code @Transactional} do JUnit não se aplica (limitação conhecida de
 * testes {@code RANDOM_PORT}).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class McpServerIntegrationTest {

    private static final List<String> EXPECTED_TOOL_NAMES = List.of(
        "add_items", "create_list", "delete_list", "get_list", "get_list_activity",
        "list_members", "list_my_lists", "remove_items", "remove_member", "rename_list",
        "set_items_checked", "share_list", "update_item"
    );

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @Autowired
    private PersonalAccessTokenService tokenService;

    @MockitoSpyBean
    private WebSocketEventPublisher eventPublisher;

    private final List<McpSyncClient> openClients = new ArrayList<>();

    @AfterEach
    void closeClients() {
        openClients.forEach(McpSyncClient::close);
        openClients.clear();
    }

    private User newUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userService.createUser(
            "mcpuser" + suffix, "mcpuser" + suffix + "@example.com", "hashed", "MCP User " + suffix,
            AuthProvider.EMAIL);
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

    private CallToolResult call(McpSyncClient client, String toolName, Map<String, Object> arguments) {
        return client.callTool(CallToolRequest.builder(toolName).arguments(arguments).build());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> structured(CallToolResult result) {
        assertSuccess(result);
        return (Map<String, Object>) result.structuredContent();
    }

    private void assertSuccess(CallToolResult result) {
        assertThat(result.isError())
            .as("tool result não deveria ser erro. Texto: %s", textOf(result))
            .isNotEqualTo(Boolean.TRUE);
    }

    private void assertError(CallToolResult result, String expectedSubstring) {
        assertThat(result.isError()).isEqualTo(Boolean.TRUE);
        assertThat(textOf(result)).contains(expectedSubstring);
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

    @Test
    @DisplayName("initialize retorna as informações do servidor nossalista-mcp")
    void initializeAdvertisesServerInfo() {
        User user = newUser();
        String token = issueToken(user, TokenScope.READ_WRITE);

        McpSyncClient client = connectedClient(token);

        assertThat(client.getServerInfo().name()).isEqualTo("nossalista-mcp");
        assertThat(client.getServerCapabilities().tools()).isNotNull();
    }

    @Test
    @DisplayName("tools/list expõe exatamente as 13 tools esperadas (snapshot de nomes)")
    void toolsListExposesExpectedTools() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user, TokenScope.READ_WRITE));

        List<String> names = client.listTools().tools().stream().map(McpSchema.Tool::name).sorted().toList();

        assertThat(names).isEqualTo(EXPECTED_TOOL_NAMES);
        // snapshot: toda tool de leitura declara outputSchema (generateOutputSchema = true)
        client.listTools().tools().forEach(tool ->
            assertThat(tool.outputSchema()).as("tool %s deveria ter outputSchema", tool.name()).isNotNull());
    }

    @Test
    @DisplayName("requisição sem token retorna 401")
    void unauthenticatedRequestReturns401() {
        RestClient restClient = RestClient.create();

        org.springframework.http.HttpStatusCode status = restClient.post()
            .uri("http://localhost:" + port + "/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            .exchange((request, response) -> response.getStatusCode(), false);

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("ciclo de vida completo de lista: create -> get (id e nome) -> rename -> list -> delete")
    void listLifecycleHappyPath() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user, TokenScope.READ_WRITE));
        String listName = "Compras MCP " + UUID.randomUUID();

        Map<String, Object> created = structured(
            call(client, "create_list", Map.of("name", listName, "type", "SHOPPING")));
        String listId = (String) created.get("id");
        assertThat(created.get("type")).isEqualTo("SHOPPING");
        assertThat(created.get("inviteCode")).isNotNull();

        Map<String, Object> byId = structured(call(client, "get_list", Map.of("listId", listId)));
        assertThat(byId.get("name")).isEqualTo(listName);
        assertThat(byId.get("owner")).isEqualTo(true);

        Map<String, Object> byName = structured(call(client, "get_list", Map.of("name", listName)));
        assertThat(byName.get("id")).isEqualTo(listId);

        String renamed = listName + " renomeada";
        Map<String, Object> rename = structured(
            call(client, "rename_list", Map.of("listId", listId, "name", renamed)));
        assertThat(rename.get("name")).isEqualTo(renamed);

        Map<String, Object> myLists = structured(call(client, "list_my_lists", Map.of()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lists = (List<Map<String, Object>>) myLists.get("lists");
        assertThat(lists).anyMatch(l -> listId.equals(l.get("id")));

        Map<String, Object> deleted = structured(call(client, "delete_list", Map.of("listId", listId)));
        assertThat(deleted.get("deleted")).isEqualTo(true);

        assertError(call(client, "get_list", Map.of("listId", listId)), "não encontrada");
    }

    @Test
    @DisplayName("add_items em lote reporta sucesso e falha por item, e itens válidos são persistidos")
    void addItemsPartialFailureReportsPerItemOutcome() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user, TokenScope.READ_WRITE));
        String listId = createShoppingList(client);

        Map<String, Object> result = structured(call(client, "add_items", Map.of(
            "listId", listId,
            "items", List.of(
                Map.of("name", "Leite", "quantity", 2),
                Map.of("name", "", "quantity", 1),
                Map.of("name", "Café")
            )
        )));

        assertThat(result.get("added")).isEqualTo(2);
        assertThat(result.get("failed")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outcomes = (List<Map<String, Object>>) result.get("results");
        assertThat(outcomes).hasSize(3);
        assertThat(outcomes.get(1).get("success")).isEqualTo(false);
        assertThat(outcomes.get(1).get("error")).isNotNull();
    }

    @Test
    @DisplayName("update_item, set_items_checked e remove_items em lote operam sobre itens existentes")
    void itemBatchToolsHappyPath() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user, TokenScope.READ_WRITE));
        String listId = createShoppingList(client);

        Map<String, Object> addResult = structured(call(client, "add_items", Map.of(
            "listId", listId,
            "items", List.of(Map.of("name", "Arroz", "quantity", 1), Map.of("name", "Feijão", "quantity", 1))
        )));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> added = (List<Map<String, Object>>) addResult.get("results");
        String item1 = (String) added.get(0).get("itemId");
        String item2 = (String) added.get(1).get("itemId");

        Map<String, Object> updated = structured(call(client, "update_item", Map.of(
            "listId", listId, "itemId", item1, "quantity", 5
        )));
        assertThat(updated.get("quantity")).isEqualTo(5);

        Map<String, Object> checkedResult = structured(call(client, "set_items_checked", Map.of(
            "listId", listId, "itemIds", List.of(item1, item2), "checked", true
        )));
        assertThat(checkedResult.get("updated")).isEqualTo(2);
        assertThat(checkedResult.get("failed")).isEqualTo(0);

        // idempotente: já checado, não deve falhar nem "des-suceder"
        Map<String, Object> checkedAgain = structured(call(client, "set_items_checked", Map.of(
            "listId", listId, "itemIds", List.of(item1), "checked", true
        )));
        assertThat(checkedAgain.get("updated")).isEqualTo(1);

        Map<String, Object> removed = structured(call(client, "remove_items", Map.of(
            "listId", listId, "itemIds", List.of(item1, item2)
        )));
        assertThat(removed.get("removed")).isEqualTo(2);
    }

    @Test
    @DisplayName("share_list (username e link), list_members e remove_member (leave) operam corretamente")
    void memberToolsHappyPath() {
        User owner = newUser();
        User invitee = newUser();
        McpSyncClient ownerClient = connectedClient(issueToken(owner, TokenScope.READ_WRITE));
        String listId = createShoppingList(ownerClient);

        Map<String, Object> shareByUsername = structured(call(ownerClient, "share_list", Map.of(
            "listId", listId, "mode", "username", "username", invitee.getUsername()
        )));
        assertThat(shareByUsername.get("invitedUsername")).isEqualTo(invitee.getUsername());

        Map<String, Object> shareByLink = structured(call(ownerClient, "share_list", Map.of(
            "listId", listId, "mode", "link"
        )));
        assertThat((String) shareByLink.get("inviteLink")).contains("/join/");

        Map<String, Object> members = structured(call(ownerClient, "list_members", Map.of("listId", listId)));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> memberList = (List<Map<String, Object>>) members.get("members");
        assertThat(memberList).extracting(m -> m.get("username")).contains(owner.getUsername(), invitee.getUsername());

        McpSyncClient inviteeClient = connectedClient(issueToken(invitee, TokenScope.READ_WRITE));
        Map<String, Object> left = structured(call(inviteeClient, "remove_member", Map.of(
            "listId", listId, "userId", invitee.getId().toString()
        )));
        assertThat(left.get("action")).isEqualTo("LEFT");
    }

    @Test
    @DisplayName("get_list_activity retorna o histórico paginado após mutações")
    void getListActivityReturnsEntries() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user, TokenScope.READ_WRITE));
        String listId = createShoppingList(client);
        call(client, "add_items", Map.of("listId", listId, "items", List.of(Map.of("name", "Item A"))));

        Map<String, Object> activity = structured(call(client, "get_list_activity", Map.of("listId", listId)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) activity.get("entries");
        assertThat(entries).isNotEmpty();
        assertThat(entries.get(0)).containsKey("action");
    }

    @Test
    @DisplayName("exceções de negócio viram isError=true com mensagem acionável, nunca stack trace")
    void businessErrorsBecomeIsErrorResults() {
        User owner = newUser();
        User stranger = newUser();
        McpSyncClient ownerClient = connectedClient(issueToken(owner, TokenScope.READ_WRITE));
        McpSyncClient strangerClient = connectedClient(issueToken(stranger, TokenScope.READ_WRITE));
        String listId = createShoppingList(ownerClient);

        assertError(call(strangerClient, "delete_list", Map.of("listId", listId)), "dono");
        assertError(call(ownerClient, "get_list", Map.of("listId", UUID.randomUUID().toString())), "não encontrada");
        assertError(call(ownerClient, "create_list", Map.of("name", "X", "type", "NOT_A_TYPE")), "type inválido");

        // nunca deve conter indícios de stack trace java
        assertThat(textOf(call(strangerClient, "delete_list", Map.of("listId", listId))))
            .doesNotContain("at br.com.leoferolive", "Exception in thread");
    }

    @Test
    @DisplayName("PAT com escopo READ pode ler mas não pode chamar tools de mutação")
    void readOnlyScopeBlocksMutationTools() {
        User user = newUser();
        McpSyncClient writeClient = connectedClient(issueToken(user, TokenScope.READ_WRITE));
        String listId = createShoppingList(writeClient);

        McpSyncClient readClient = connectedClient(issueToken(user, TokenScope.READ));

        assertSuccess(call(readClient, "list_my_lists", Map.of()));
        assertSuccess(call(readClient, "get_list", Map.of("listId", listId)));
        assertSuccess(call(readClient, "list_members", Map.of("listId", listId)));
        assertSuccess(call(readClient, "get_list_activity", Map.of("listId", listId)));

        assertError(call(readClient, "create_list", Map.of("name", "X", "type", "SHOPPING")), "read-only scope");
        assertError(call(readClient, "rename_list", Map.of("listId", listId, "name", "Y")), "read-only scope");
        assertError(call(readClient, "delete_list", Map.of("listId", listId)), "read-only scope");
        assertError(
            call(readClient, "add_items", Map.of("listId", listId, "items", List.of(Map.of("name", "X")))),
            "read-only scope");
    }

    @Test
    @DisplayName("isolamento multi-tenant: usuário B não vê nem edita lista do usuário A via MCP")
    void multiTenantIsolation() {
        User userA = newUser();
        User userB = newUser();
        McpSyncClient clientA = connectedClient(issueToken(userA, TokenScope.READ_WRITE));
        McpSyncClient clientB = connectedClient(issueToken(userB, TokenScope.READ_WRITE));
        String listAId = createShoppingList(clientA);

        assertError(call(clientB, "get_list", Map.of("listId", listAId)), "permissão");
        assertError(call(clientB, "add_items", Map.of(
            "listId", listAId, "items", List.of(Map.of("name", "invasor")))), "permissão");
        assertError(call(clientB, "delete_list", Map.of("listId", listAId)), "dono");

        Map<String, Object> myListsB = structured(call(clientB, "list_my_lists", Map.of()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listsB = (List<Map<String, Object>>) myListsB.get("lists");
        assertThat(listsB).noneMatch(l -> listAId.equals(l.get("id")));
    }

    @Test
    @DisplayName("mutação via MCP publica evento no tópico STOMP da lista")
    void mutationPublishesWebSocketBroadcast() {
        User user = newUser();
        McpSyncClient client = connectedClient(issueToken(user, TokenScope.READ_WRITE));
        String listId = createShoppingList(client);

        call(client, "add_items", Map.of("listId", listId, "items", List.of(Map.of("name", "Transmitido"))));

        verify(eventPublisher, atLeastOnce())
            .publishEvent(eq(UUID.fromString(listId)), any(), any(), any(), any());
    }

    private String createShoppingList(McpSyncClient client) {
        Map<String, Object> created = structured(call(client, "create_list", Map.of(
            "name", "Lista MCP " + UUID.randomUUID(), "type", "SHOPPING"
        )));
        return (String) created.get("id");
    }
}
