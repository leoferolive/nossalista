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
}
