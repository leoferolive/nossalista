package br.com.leoferolive.nossalista.listitem.controller;

import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import br.com.leoferolive.nossalista.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para ListItemController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class ListItemControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListService listService;

    @Autowired
    private ListRepository listRepository;

    @Autowired
    private ListItemRepository listItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private User testUser;
    private User otherUser;

    /**
     * Configura MockMvc com Spring Security e cria usuários de teste
     */
    @BeforeEach
    void setUp() {
        // Configurar MockMvc com SecurityContext
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())  // Aplica filtros de segurança
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Para LocalDateTime

        // Limpar SecurityContext antes de cada teste
        SecurityContextHolder.clearContext();

        // Criar usuário de teste
        testUser = userService.createUser(
                "testitemuser",
                "testitem@example.com",
                "hashedPassword",
                "Test Item User",
                AuthProvider.EMAIL
        );

        // Criar outro usuário (para testes de permissão)
        otherUser = userService.createUser(
                "otheruser",
                "other@example.com",
                "hashedPassword",
                "Other User",
                AuthProvider.EMAIL
        );
    }

    /**
     * Autentica um usuário no SecurityContext para simular requisição autenticada
     */
    private void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        java.util.Collections.emptyList()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Nested
    @DisplayName("POST /api/lists/{listId}/items - Adicionar Item")
    class AddItemTests {

        @Test
        @DisplayName("Deve criar item com sucesso quando usuário é dono")
        void shouldCreateItemWhenUserIsOwner() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            Map<String, Object> request = new HashMap<>();
            request.put("name", "Arroz");
            request.put("quantity", 2);

            // When & Then
            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Arroz"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.position").value(0))
                .andExpect(jsonPath("$.checked").value(false))
                .andExpect(jsonPath("$.createdBy.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.createdBy.username").value("testitemuser"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

            // Verify item was created in database
            var items = listItemRepository.findByListIdOrderByPositionAsc(testList.getId());
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getName()).isEqualTo("Arroz");
            assertThat(items.get(0).getQuantity()).isEqualTo(2);
            assertThat(items.get(0).getPosition()).isEqualTo(0);
            assertThat(items.get(0).getCreatedBy().getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("Deve retornar 404 quando lista não existe")
        void shouldReturn404WhenListDoesNotExist() throws Exception {
            // Arrange
            authenticateUser(testUser);
            UUID nonExistentId = UUID.randomUUID();
            Map<String, Object> request = new HashMap<>();
            request.put("name", "Arroz");

            // When & Then
            mockMvc.perform(post("/api/lists/{listId}/items", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/list-not-found"))
                .andExpect(jsonPath("$.title").value("Lista Não Encontrada"))
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário não é participante")
        void shouldReturn403WhenUserIsNotParticipant() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista Privada", 1), testUser);

            // Limpar e autenticar como outro usuário
            SecurityContextHolder.clearContext();
            authenticateUser(otherUser);

            Map<String, Object> request = new HashMap<>();
            request.put("name", "Arroz");

            // When & Then
            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/access-forbidden"))
                .andExpect(jsonPath("$.title").value("Acesso Negado"))
                .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome está vazio")
        void shouldReturn400WhenNameIsEmpty() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            Map<String, Object> request = new HashMap<>();
            request.put("name", "");

            // When & Then
            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").exists());
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome tem mais de 200 caracteres")
        void shouldReturn400WhenNameIsTooLong() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            Map<String, Object> request = new HashMap<>();
            request.put("name", "A".repeat(201));

            // When & Then
            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/validation-error"))
                .andExpect(jsonPath("$.errors.name").exists());
        }

        @Test
        @DisplayName("Deve retornar 401 quando não há token")
        void shouldReturn401WhenNoToken() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            // Limpar autenticação
            SecurityContextHolder.clearContext();

            Map<String, Object> request = new HashMap<>();
            request.put("name", "Arroz");

            // When & Then
            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve calcular position corretamente para múltiplos itens")
        void shouldCalculatePositionCorrectlyForMultipleItems() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            // Create first item
            Map<String, Object> request1 = new HashMap<>();
            request1.put("name", "Primeiro Item");

            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(0));

            // Create second item
            Map<String, Object> request2 = new HashMap<>();
            request2.put("name", "Segundo Item");

            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(1));

            // Create third item
            Map<String, Object> request3 = new HashMap<>();
            request3.put("name", "Terceiro Item");

            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(2));

            // Verify in database
            var items = listItemRepository.findByListIdOrderByPositionAsc(testList.getId());
            assertThat(items).hasSize(3);
            assertThat(items.get(0).getPosition()).isEqualTo(0);
            assertThat(items.get(1).getPosition()).isEqualTo(1);
            assertThat(items.get(2).getPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("Deve aceitar campo quantity em lista tipo Shopping")
        void shouldAcceptDynamicFields() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Compras", 1), testUser); // SHOPPING

            Map<String, Object> request = new HashMap<>();
            request.put("name", "Arroz Integral");
            request.put("quantity", 5);

            // When & Then
            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Arroz Integral"))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.dueDate").doesNotExist())
                .andExpect(jsonPath("$.url").doesNotExist());
        }

        @Test
        @DisplayName("Deve definir checked como false por padrão")
        void shouldSetCheckedAsFalseByDefault() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            Map<String, Object> request = new HashMap<>();
            request.put("name", "Item Não Marcado");

            // When & Then
            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checked").value(false));

            // Verify in database
            var items = listItemRepository.findByListIdOrderByPositionAsc(testList.getId());
            assertThat(items.get(0).isChecked()).isFalse();
        }

        @Test
        @DisplayName("Deve fazer trim no nome do item")
        void shouldTrimItemName() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            Map<String, Object> request = new HashMap<>();
            request.put("name", "  Arroz Integral  ");

            // When & Then
            mockMvc.perform(post("/api/lists/{listId}/items", testList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Arroz Integral"));

            // Verify in database
            var items = listItemRepository.findByListIdOrderByPositionAsc(testList.getId());
            assertThat(items.get(0).getName()).isEqualTo("Arroz Integral");
        }
    }

    @Nested
    @DisplayName("GET /api/lists/{listId}/items - Listar Itens")
    class GetItemsTests {

        @Test
        @DisplayName("Deve retornar itens ordenados por position quando usuário é dono")
        void shouldReturnItemsOrderedByPosition() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            // Criar 3 itens
            createItem(testList, "Item 1", 0);
            createItem(testList, "Item 2", 1);
            createItem(testList, "Item 3", 2);

            // When & Then
            mockMvc.perform(get("/api/lists/{listId}/items", testList.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Item 1"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].name").value("Item 2"))
                .andExpect(jsonPath("$[1].position").value(1))
                .andExpect(jsonPath("$[2].name").value("Item 3"))
                .andExpect(jsonPath("$[2].position").value(2));
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há itens")
        void shouldReturnEmptyListWhenNoItems() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista Vazia", 1), testUser);

            // When & Then
            mockMvc.perform(get("/api/lists/{listId}/items", testList.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Deve retornar 404 quando lista não existe")
        void shouldReturn404WhenListDoesNotExist() throws Exception {
            // Arrange
            authenticateUser(testUser);
            UUID nonExistentId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(get("/api/lists/{listId}/items", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/list-not-found"))
                .andExpect(jsonPath("$.title").value("Lista Não Encontrada"))
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário não é participante")
        void shouldReturn403WhenUserIsNotParticipant() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista Privada", 1), testUser);

            // Limpar e autenticar como outro usuário
            SecurityContextHolder.clearContext();
            authenticateUser(otherUser);

            // When & Then
            mockMvc.perform(get("/api/lists/{listId}/items", testList.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/access-forbidden"))
                .andExpect(jsonPath("$.title").value("Acesso Negado"))
                .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Deve retornar 401 quando não há token")
        void shouldReturn401WhenNoToken() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            // Limpar autenticação
            SecurityContextHolder.clearContext();

            // When & Then
            mockMvc.perform(get("/api/lists/{listId}/items", testList.getId()))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve retornar campos completos de cada item")
        void shouldReturnCompleteItemFields() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Compras", 1), testUser);

            ListItem item = new ListItem();
            item.setName("Arroz Integral");
            item.setList(testList);
            item.setCreatedBy(testUser);
            item.setPosition(0);
            item.setChecked(false);
            item.setQuantity(5);
            listItemRepository.save(item);

            // When & Then
            mockMvc.perform(get("/api/lists/{listId}/items", testList.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").value("Arroz Integral"))
                .andExpect(jsonPath("$[0].checked").value(false))
                .andExpect(jsonPath("$[0].quantity").value(5))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[0].createdBy.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$[0].createdBy.username").value("testitemuser"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists());
        }

        private void createItem(List list, String name, int position) {
            ListItem item = new ListItem();
            item.setName(name);
            item.setList(list);
            item.setCreatedBy(testUser);
            item.setPosition(position);
            item.setChecked(false);
            listItemRepository.save(item);
        }
    }

    @Nested
    @DisplayName("PATCH /api/lists/{listId}/items/{itemId}/check - Toggle Item Check")
    class ToggleItemCheckTests {

        @Test
        @DisplayName("Deve inverter checked de false para true")
        void shouldToggleCheckedFromFalseToTrue() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            ListItem item = new ListItem();
            item.setName("Item Teste");
            item.setList(testList);
            item.setCreatedBy(testUser);
            item.setPosition(0);
            item.setChecked(false);
            listItemRepository.save(item);

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}/check", testList.getId(), item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId().toString()))
                .andExpect(jsonPath("$.name").value("Item Teste"))
                .andExpect(jsonPath("$.checked").value(true));

            // Verify in database
            var updatedItem = listItemRepository.findById(item.getId()).orElseThrow();
            assertThat(updatedItem.isChecked()).isTrue();
        }

        @Test
        @DisplayName("Deve inverter checked de true para false")
        void shouldToggleCheckedFromTrueToFalse() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            ListItem item = new ListItem();
            item.setName("Item Teste");
            item.setList(testList);
            item.setCreatedBy(testUser);
            item.setPosition(0);
            item.setChecked(true); // Começa como true
            listItemRepository.save(item);

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}/check", testList.getId(), item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(false));

            // Verify in database
            var updatedItem = listItemRepository.findById(item.getId()).orElseThrow();
            assertThat(updatedItem.isChecked()).isFalse();
        }

        @Test
        @DisplayName("Deve retornar 404 quando lista não existe")
        void shouldReturn404WhenListDoesNotExist() throws Exception {
            // Arrange
            authenticateUser(testUser);
            UUID nonExistentListId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}/check", nonExistentListId, itemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/list-not-found"))
                .andExpect(jsonPath("$.title").value("Lista Não Encontrada"))
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Deve retornar 404 quando item não existe")
        void shouldReturn404WhenItemDoesNotExist() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);
            UUID nonExistentItemId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}/check", testList.getId(), nonExistentItemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/item-not-found"))
                .andExpect(jsonPath("$.title").value("Item Não Encontrado"))
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário não é participante")
        void shouldReturn403WhenUserIsNotParticipant() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista Privada", 1), testUser);

            ListItem item = new ListItem();
            item.setName("Item Teste");
            item.setList(testList);
            item.setCreatedBy(testUser);
            item.setPosition(0);
            item.setChecked(false);
            listItemRepository.save(item);

            // Limpar e autenticar como outro usuário
            SecurityContextHolder.clearContext();
            authenticateUser(otherUser);

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}/check", testList.getId(), item.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/access-forbidden"))
                .andExpect(jsonPath("$.title").value("Acesso Negado"))
                .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Deve retornar 401 quando não há token")
        void shouldReturn401WhenNoToken() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            ListItem item = new ListItem();
            item.setName("Item Teste");
            item.setList(testList);
            item.setCreatedBy(testUser);
            item.setPosition(0);
            item.setChecked(false);
            listItemRepository.save(item);

            // Limpar autenticação
            SecurityContextHolder.clearContext();

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}/check", testList.getId(), item.getId()))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve retornar 404 quando item não pertence à lista")
        void shouldReturn404WhenItemDoesNotBelongToList() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List testList = listService.createList(new CreateListRequest("Lista 1", 1), testUser);
            List otherList = listService.createList(new CreateListRequest("Lista 2", 1), testUser);

            ListItem item = new ListItem();
            item.setName("Item da Lista 2");
            item.setList(otherList);
            item.setCreatedBy(testUser);
            item.setPosition(0);
            item.setChecked(false);
            listItemRepository.save(item);

            // Act & Assert - Tentar acessar item da Lista 2 através da Lista 1
            mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}/check", testList.getId(), item.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/item-not-found"))
                .andExpect(jsonPath("$.title").value("Item Não Encontrado"))
                .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("DELETE /api/lists/{listId}/items/{itemId} - Remover Item")
    class DeleteItemTests {

        @Test
        @DisplayName("Deve deletar item com sucesso e retornar 204")
        void shouldDeleteItemAndReturn204() throws Exception {
            // Arrange
            authenticateUser(testUser);
            br.com.leoferolive.nossalista.list.domain.List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            ListItem item = new ListItem();
            item.setName("Item para Deletar");
            item.setList(testList);
            item.setCreatedBy(testUser);
            item.setPosition(0);
            item.setChecked(false);
            listItemRepository.save(item);

            // Act & Assert
            mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", testList.getId(), item.getId()))
                .andExpect(status().isNoContent());

            // Verify item was deleted
            var items = listItemRepository.findByListIdOrderByPositionAsc(testList.getId());
            assertThat(items).isEmpty();
        }

        @Test
        @DisplayName("Deve reordenar positions após deletar item")
        void shouldReorderPositionsAfterDelete() throws Exception {
            // Arrange
            authenticateUser(testUser);
            br.com.leoferolive.nossalista.list.domain.List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            // Criar 3 itens
            ListItem item1 = createItem(testList, "Item 1", 0);
            ListItem item2 = createItem(testList, "Item 2", 1);
            ListItem item3 = createItem(testList, "Item 3", 2);

            // Act - Deletar item do meio (position 1)
            mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", testList.getId(), item2.getId()))
                .andExpect(status().isNoContent());

            // Assert - Verificar reordenação
            var items = listItemRepository.findByListIdOrderByPositionAsc(testList.getId());
            assertThat(items).hasSize(2);
            assertThat(items.get(0).getName()).isEqualTo("Item 1");
            assertThat(items.get(0).getPosition()).isEqualTo(0);
            assertThat(items.get(1).getName()).isEqualTo("Item 3");
            assertThat(items.get(1).getPosition()).isEqualTo(1); // Reordenado de 2 para 1
        }

        @Test
        @DisplayName("Deve retornar 404 quando lista não existe")
        void shouldReturn404WhenListDoesNotExist() throws Exception {
            // Arrange
            authenticateUser(testUser);
            UUID nonExistentListId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", nonExistentListId, itemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/list-not-found"))
                .andExpect(jsonPath("$.title").value("Lista Não Encontrada"))
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Deve retornar 404 quando item não existe")
        void shouldReturn404WhenItemDoesNotExist() throws Exception {
            // Arrange
            authenticateUser(testUser);
            br.com.leoferolive.nossalista.list.domain.List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);
            UUID nonExistentItemId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", testList.getId(), nonExistentItemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/item-not-found"))
                .andExpect(jsonPath("$.title").value("Item Não Encontrado"))
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário não é participante")
        void shouldReturn403WhenUserIsNotParticipant() throws Exception {
            // Arrange
            authenticateUser(testUser);
            br.com.leoferolive.nossalista.list.domain.List testList = listService.createList(new CreateListRequest("Lista Privada", 1), testUser);

            ListItem item = new ListItem();
            item.setName("Item Teste");
            item.setList(testList);
            item.setCreatedBy(testUser);
            item.setPosition(0);
            item.setChecked(false);
            listItemRepository.save(item);

            // Limpar e autenticar como outro usuário
            SecurityContextHolder.clearContext();
            authenticateUser(otherUser);

            // Act & Assert
            mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", testList.getId(), item.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/access-forbidden"))
                .andExpect(jsonPath("$.title").value("Acesso Negado"))
                .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Deve retornar 401 quando não há token")
        void shouldReturn401WhenNoToken() throws Exception {
            // Arrange
            authenticateUser(testUser);
            br.com.leoferolive.nossalista.list.domain.List testList = listService.createList(new CreateListRequest("Lista de Teste", 1), testUser);

            ListItem item = new ListItem();
            item.setName("Item Teste");
            item.setList(testList);
            item.setCreatedBy(testUser);
            item.setPosition(0);
            item.setChecked(false);
            listItemRepository.save(item);

            // Limpar autenticação
            SecurityContextHolder.clearContext();

            // Act & Assert
            mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", testList.getId(), item.getId()))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve retornar 404 quando item não pertence à lista")
        void shouldReturn404WhenItemDoesNotBelongToList() throws Exception {
            // Arrange
            authenticateUser(testUser);
            br.com.leoferolive.nossalista.list.domain.List testList = listService.createList(new CreateListRequest("Lista 1", 1), testUser);
            br.com.leoferolive.nossalista.list.domain.List otherList = listService.createList(new CreateListRequest("Lista 2", 1), testUser);

            ListItem item = new ListItem();
            item.setName("Item da Lista 2");
            item.setList(otherList);
            item.setCreatedBy(testUser);
            item.setPosition(0);
            item.setChecked(false);
            listItemRepository.save(item);

            // Act & Assert - Tentar deletar item da Lista 2 através da Lista 1
            mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", testList.getId(), item.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/item-not-found"))
                .andExpect(jsonPath("$.title").value("Item Não Encontrado"))
                .andExpect(jsonPath("$.status").value(404));
        }

        private ListItem createItem(br.com.leoferolive.nossalista.list.domain.List list, String name, int position) {
            ListItem item = new ListItem();
            item.setName(name);
            item.setList(list);
            item.setCreatedBy(testUser);
            item.setPosition(position);
            item.setChecked(false);
            return listItemRepository.save(item);
        }
    }
}
