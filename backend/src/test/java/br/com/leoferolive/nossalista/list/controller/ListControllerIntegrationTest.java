package br.com.leoferolive.nossalista.list.controller;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.dto.UpdateListNameRequest;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para ListController
 * Testa a stack completa (controller + service + repository + DB H2)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class ListControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private ListService listService;

    @Autowired
    private ListRepository listRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;

    /**
     * Configura MockMvc com Spring Security e cria usuário de teste
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
                "listowner",
                "listowner@example.com",
                "hashedPassword",
                "List Owner",
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
    @DisplayName("POST /api/lists - Criar Lista")
    class CreateListTests {

        @Test
        @DisplayName("Deve criar lista quando autenticado - retorna 201 com dados completos")
        void shouldCreateListWhenAuthenticated() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request = new CreateListRequest("Mercado Semanal", 1);

            // Act
            MvcResult result = mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Mercado Semanal"))
                    .andExpect(jsonPath("$.type.id").value(1))
                    .andExpect(jsonPath("$.type.slug").value("compras"))
                    .andExpect(jsonPath("$.owner.id").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.owner.username").value("listowner"))
                    .andExpect(jsonPath("$.inviteCode").exists())
                    .andExpect(jsonPath("$.inviteCode").isString())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andReturn();

            // Assert - verificar que foi salvo no banco
            String responseJson = result.getResponse().getContentAsString();
            UUID listId = UUID.fromString(
                    objectMapper.readTree(responseJson).get("id").asText()
            );

            List savedList = listRepository.findById(listId).orElseThrow();
            assertEquals("Mercado Semanal", savedList.getName());
            assertEquals(1, savedList.getTypeId());
            assertEquals(testUser.getId(), savedList.getOwner().getId());
            assertNotNull(savedList.getInviteCode());
            assertEquals(12, savedList.getInviteCode().length());
        }

        @Test
        @DisplayName("Deve criar lista de Tarefas (typeId=2) com sucesso")
        void shouldCreateTaskListSuccessfully() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request = new CreateListRequest("Tarefas do Projeto", 2);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Tarefas do Projeto"))
                    .andExpect(jsonPath("$.type.id").value(2))
                    .andExpect(jsonPath("$.type.slug").value("tarefas"));

            // Verificar persistência
            assertEquals(1, listRepository.count());
        }

        @Test
        @DisplayName("Deve criar lista de Wishlist (typeId=3) com sucesso")
        void shouldCreateWishlistSuccessfully() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request = new CreateListRequest("Presentes de Aniversário", 3);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Presentes de Aniversário"))
                    .andExpect(jsonPath("$.type.id").value(3))
                    .andExpect(jsonPath("$.type.slug").value("wishlist"));

            // Verificar persistência
            assertEquals(1, listRepository.count());
        }

        @Test
        @DisplayName("Deve criar lista Genérica (typeId=4) com sucesso")
        void shouldCreateGenericListSuccessfully() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request = new CreateListRequest("Lista Geral", 4);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Lista Geral"))
                    .andExpect(jsonPath("$.type.id").value(4))
                    .andExpect(jsonPath("$.type.slug").value("generica"));

            // Verificar persistência
            assertEquals(1, listRepository.count());
        }

        @Test
        @DisplayName("Deve gerar inviteCode único para cada lista")
        void shouldGenerateUniqueInviteCodeForEachList() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request1 = new CreateListRequest("Lista 1", 1);
            CreateListRequest request2 = new CreateListRequest("Lista 2", 1);

            // Act - criar duas listas
            MvcResult result1 = mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated())
                    .andReturn();

            MvcResult result2 = mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Assert - verificar que inviteCodes são diferentes
            String inviteCode1 = objectMapper.readTree(
                    result1.getResponse().getContentAsString()
            ).get("inviteCode").asText();

            String inviteCode2 = objectMapper.readTree(
                    result2.getResponse().getContentAsString()
            ).get("inviteCode").asText();

            assertNotEquals(inviteCode1, inviteCode2, "InviteCodes devem ser únicos");
            assertEquals(12, inviteCode1.length(), "InviteCode deve ter 12 caracteres");
            assertEquals(12, inviteCode2.length(), "InviteCode deve ter 12 caracteres");
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome tem menos de 3 caracteres")
        void shouldReturn400WhenNameTooShort() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request = new CreateListRequest("AB", 1);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // Verificar que nenhuma lista foi criada
            assertEquals(0, listRepository.count());
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome está vazio")
        void shouldReturn400WhenNameIsEmpty() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request = new CreateListRequest("", 1);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // Verificar que nenhuma lista foi criada
            assertEquals(0, listRepository.count());
        }

        @Test
        @DisplayName("Deve retornar 400 quando typeId excede limite permitido")
        void shouldReturn400WhenInvalidListType() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request = new CreateListRequest("Minha Lista", 999);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").exists());

            // Verificar que nenhuma lista foi criada
            assertEquals(0, listRepository.count());
        }

        @Test
        @DisplayName("Deve retornar 400 quando typeId é 0")
        void shouldReturn400WhenTypeIdIsZero() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request = new CreateListRequest("Lista Teste", 0);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // Verificar que nenhuma lista foi criada
            assertEquals(0, listRepository.count());
        }

        @Test
        @DisplayName("Deve retornar 401 quando não autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange - não autentica usuário
            CreateListRequest request = new CreateListRequest("Lista Sem Auth", 1);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            // Verificar que nenhuma lista foi criada
            assertEquals(0, listRepository.count());
        }

        @Test
        @DisplayName("Deve aceitar nome no limite máximo de 100 caracteres")
        void shouldAcceptNameAtMaxLength() throws Exception {
            // Arrange
            authenticateUser(testUser);
            String longName = "A".repeat(100); // Exatamente 100 caracteres
            CreateListRequest request = new CreateListRequest(longName, 1);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(longName));

            // Verificar persistência
            assertEquals(1, listRepository.count());
            List savedList = listRepository.findAll().get(0);
            assertEquals(100, savedList.getName().length());
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome excede 100 caracteres")
        void shouldReturn400WhenNameExceedsMaxLength() throws Exception {
            // Arrange
            authenticateUser(testUser);
            String tooLongName = "A".repeat(101); // 101 caracteres
            CreateListRequest request = new CreateListRequest(tooLongName, 1);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // Verificar que nenhuma lista foi criada
            assertEquals(0, listRepository.count());
        }

        @Test
        @DisplayName("Deve aceitar nome no limite mínimo de 3 caracteres")
        void shouldAcceptNameAtMinLength() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request = new CreateListRequest("ABC", 1);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("ABC"));

            // Verificar persistência
            assertEquals(1, listRepository.count());
        }

        @Test
        @DisplayName("Deve criar lista com caracteres especiais no nome")
        void shouldCreateListWithSpecialCharactersInName() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request = new CreateListRequest("Lista #1 - Tarefas & Compras!", 1);

            // Act & Assert
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Lista #1 - Tarefas & Compras!"));

            // Verificar persistência
            assertEquals(1, listRepository.count());
        }

        @Test
        @DisplayName("Deve criar listas diferentes para o mesmo usuário")
        void shouldCreateMultipleListsForSameUser() throws Exception {
            // Arrange
            authenticateUser(testUser);
            CreateListRequest request1 = new CreateListRequest("Lista 1", 1);
            CreateListRequest request2 = new CreateListRequest("Lista 2", 2);
            CreateListRequest request3 = new CreateListRequest("Lista 3", 3);

            // Act - criar três listas
            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request3)))
                    .andExpect(status().isCreated());

            // Assert - verificar que todas foram criadas
            assertEquals(3, listRepository.count());
            assertEquals(3, listRepository.findByOwnerId(testUser.getId()).size());
        }
    }

    @Nested
    @DisplayName("GET /api/lists - Listar Listas")
    class GetAllListsTests {

        @Test
        @DisplayName("Deve retornar 200 OK com array vazio quando usuário não tem listas")
        void shouldReturn200WithEmptyArrayWhenUserHasNoLists() throws Exception {
            // Arrange
            authenticateUser(testUser);

            // Act & Assert
            mockMvc.perform(get("/api/lists"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Deve retornar 200 OK com array de listas quando usuário tem listas")
        void shouldReturn200WithListsArrayWhenUserHasLists() throws Exception {
            // Arrange
            authenticateUser(testUser);
            listService.createList(new CreateListRequest("Mercado Semanal", 1), testUser);
            listService.createList(new CreateListRequest("Tarefas do Projeto", 2), testUser);
            listService.createList(new CreateListRequest("Wishlist Aniversário", 3), testUser);

            // Act & Assert
            mockMvc.perform(get("/api/lists"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].name").exists())
                    .andExpect(jsonPath("$[0].type.id").exists())
                    .andExpect(jsonPath("$[0].type.slug").exists())
                    .andExpect(jsonPath("$[0].owner.id").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$[0].owner.username").value("listowner"))
                    .andExpect(jsonPath("$[0].owner.name").value("List Owner"))
                    .andExpect(jsonPath("$[0].inviteCode").exists())
                    .andExpect(jsonPath("$[0].isOwner").value(true))
                    .andExpect(jsonPath("$[0].itemsCount").value(0))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].updatedAt").exists());
        }

        @Test
        @DisplayName("Deve retornar 401 quando não autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange - não autentica usuário

            // Act & Assert
            mockMvc.perform(get("/api/lists"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve retornar listas ordenadas por updatedAt DESC")
        void shouldReturnListsOrderedByUpdatedAtDesc() throws Exception {
            // Arrange
            authenticateUser(testUser);

            // Criar 3 listas em ordem (primeira criada terá updatedAt mais antigo)
            List list1 = listService.createList(new CreateListRequest("Lista 1", 1), testUser);
            Thread.sleep(10); // Pequeno delay para garantir updatedAt diferente
            List list2 = listService.createList(new CreateListRequest("Lista 2", 2), testUser);
            Thread.sleep(10);
            List list3 = listService.createList(new CreateListRequest("Lista 3", 3), testUser);

            // Act & Assert - a mais recente deve vir primeiro
            mockMvc.perform(get("/api/lists"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].id").value(list3.getId().toString()))
                    .andExpect(jsonPath("$[1].id").value(list2.getId().toString()))
                    .andExpect(jsonPath("$[2].id").value(list1.getId().toString()));
        }

        @Test
        @DisplayName("Deve verificar que isOwner é true quando usuário é dono")
        void shouldVerifyIsOwnerTrueWhenUserIsOwner() throws Exception {
            // Arrange
            authenticateUser(testUser);
            listService.createList(new CreateListRequest("Minha Lista", 1), testUser);

            // Act & Assert
            mockMvc.perform(get("/api/lists"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].isOwner").value(true))
                    .andExpect(jsonPath("$[0].owner.id").value(testUser.getId().toString()));
        }

        @Test
        @DisplayName("Deve incluir todos os tipos de lista corretamente")
        void shouldIncludeAllListTypesCorrectly() throws Exception {
            // Arrange
            authenticateUser(testUser);
            listService.createList(new CreateListRequest("Compras", 1), testUser);
            listService.createList(new CreateListRequest("Tarefas", 2), testUser);
            listService.createList(new CreateListRequest("Wishlist", 3), testUser);
            listService.createList(new CreateListRequest("Genérica", 4), testUser);

            // Act & Assert
            mockMvc.perform(get("/api/lists"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$[*].type.id").exists())
                    .andExpect(jsonPath("$[*].type.slug").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/lists/{id} - Obter Detalhes da Lista")
    class GetListByIdTests {

        @Test
        @DisplayName("Deve retornar 200 OK com dados completos quando lista existe e usuário é owner")
        void shouldReturn200WithCompleteDataWhenListExistsAndUserIsOwner() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Minha Lista Detalhada", 1), testUser);

            // Act & Assert
            mockMvc.perform(get("/api/lists/{id}", createdList.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(createdList.getId().toString()))
                    .andExpect(jsonPath("$.name").value("Minha Lista Detalhada"))
                    .andExpect(jsonPath("$.type.id").value(1))
                    .andExpect(jsonPath("$.type.slug").value("compras"))
                    .andExpect(jsonPath("$.owner.id").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.owner.username").value("listowner"))
                    .andExpect(jsonPath("$.owner.name").value("List Owner"))
                    .andExpect(jsonPath("$.inviteCode").exists())
                    .andExpect(jsonPath("$.isOwner").value(true))
                    .andExpect(jsonPath("$.itemsCount").value(0))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Deve retornar 404 quando lista não existe")
        void shouldReturn404WhenListDoesNotExist() throws Exception {
            // Arrange
            authenticateUser(testUser);
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(get("/api/lists/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Lista Não Encontrada"))
                    .andExpect(jsonPath("$.detail").value("Lista não encontrada"));
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário não tem permissão (não é owner)")
        void shouldReturn403WhenUserHasNoPermission() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Privada", 1), testUser);

            // Criar outro usuário
            User otherUser = userService.createUser(
                    "otheruser",
                    "other@example.com",
                    "hashedPassword",
                    "Other User",
                    AuthProvider.EMAIL
            );

            // Autenticar como outro usuário
            authenticateUser(otherUser);

            // Act & Assert - tentar acessar lista do primeiro usuário
            mockMvc.perform(get("/api/lists/{id}", createdList.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Acesso Negado"))
                    .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar esta lista"));
        }

        @Test
        @DisplayName("Deve retornar 401 quando não autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange - criar lista sem autenticar
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Teste", 1), testUser);

            // Limpar autenticação
            SecurityContextHolder.clearContext();

            // Act & Assert
            mockMvc.perform(get("/api/lists/{id}", createdList.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve retornar lista de Tarefas (typeId=2) corretamente")
        void shouldReturnTaskListSuccessfully() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Tarefas Importantes", 2), testUser);

            // Act & Assert
            mockMvc.perform(get("/api/lists/{id}", createdList.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Tarefas Importantes"))
                    .andExpect(jsonPath("$.type.id").value(2))
                    .andExpect(jsonPath("$.type.slug").value("tarefas"))
                    .andExpect(jsonPath("$.isOwner").value(true));
        }

        @Test
        @DisplayName("Deve retornar Wishlist (typeId=3) corretamente")
        void shouldReturnWishlistSuccessfully() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Presentes Desejados", 3), testUser);

            // Act & Assert
            mockMvc.perform(get("/api/lists/{id}", createdList.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Presentes Desejados"))
                    .andExpect(jsonPath("$.type.id").value(3))
                    .andExpect(jsonPath("$.type.slug").value("wishlist"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/lists/{id} - Atualizar Nome da Lista")
    class UpdateListNameTests {

        @Test
        @DisplayName("Deve retornar 200 OK quando atualiza nome com sucesso")
        void shouldReturn200WhenUpdateNameSuccessfully() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Nome Original", 1), testUser);

            UpdateListNameRequest request = new UpdateListNameRequest("Novo Nome da Lista");

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{id}", createdList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(createdList.getId().toString()))
                    .andExpect(jsonPath("$.name").value("Novo Nome da Lista"))
                    .andExpect(jsonPath("$.type.id").value(1))
                    .andExpect(jsonPath("$.isOwner").value(true));
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome tem menos de 3 caracteres")
        void shouldReturn400WhenNameTooShort() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Teste", 1), testUser);

            UpdateListNameRequest request = new UpdateListNameRequest("AB");

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{id}", createdList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome está vazio")
        void shouldReturn400WhenNameIsEmpty() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Teste", 1), testUser);

            UpdateListNameRequest request = new UpdateListNameRequest("");

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{id}", createdList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário não é dono da lista")
        void shouldReturn403WhenUserIsNotOwner() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Privada", 1), testUser);

            // Criar outro usuário
            User otherUser = userService.createUser(
                    "otheruser",
                    "other@example.com",
                    "hashedPassword",
                    "Other User",
                    AuthProvider.EMAIL
            );

            // Autenticar como outro usuário
            authenticateUser(otherUser);

            UpdateListNameRequest request = new UpdateListNameRequest("Tentativa de Renomear");

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{id}", createdList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Acesso Negado"))
                    .andExpect(jsonPath("$.detail").value("Apenas o dono pode editar esta lista"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando lista não existe")
        void shouldReturn404WhenListDoesNotExist() throws Exception {
            // Arrange
            authenticateUser(testUser);
            UUID nonExistentId = UUID.randomUUID();

            UpdateListNameRequest request = new UpdateListNameRequest("Novo Nome");

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Lista Não Encontrada"))
                    .andExpect(jsonPath("$.detail").value("Lista não encontrada"));
        }

        @Test
        @DisplayName("Deve retornar 401 quando não autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Teste", 1), testUser);

            // Limpar autenticação
            SecurityContextHolder.clearContext();

            UpdateListNameRequest request = new UpdateListNameRequest("Novo Nome");

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{id}", createdList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve aceitar nome no limite máximo de 100 caracteres")
        void shouldAcceptNameAtMaxLength() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Teste", 1), testUser);

            String longName = "A".repeat(100);
            UpdateListNameRequest request = new UpdateListNameRequest(longName);

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{id}", createdList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(longName));
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome excede 100 caracteres")
        void shouldReturn400WhenNameExceedsMaxLength() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Teste", 1), testUser);

            String tooLongName = "A".repeat(101);
            UpdateListNameRequest request = new UpdateListNameRequest(tooLongName);

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{id}", createdList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve ignorar campos desconhecidos no request body")
        void shouldIgnoreUnknownFieldsInRequestBody() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Teste", 1), testUser);

            // Enviar JSON com campo extra "typeId" que deve ser ignorado
            String jsonWithExtraField = "{\"name\": \"Novo Nome\", \"typeId\": 2}";

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{id}", createdList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithExtraField))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Novo Nome"))
                    .andExpect(jsonPath("$.type.id").value(1)); // Tipo não deve mudar
        }

        @Test
        @DisplayName("Deve persistir novo nome no banco de dados")
        void shouldPersistNewNameInDatabase() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Original", 1), testUser);

            UpdateListNameRequest request = new UpdateListNameRequest("Lista Renomeada Persistida");

            // Act & Assert
            mockMvc.perform(patch("/api/lists/{id}", createdList.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Lista Renomeada Persistida"));

            // Verificar persistência no banco
            List updatedList = listRepository.findById(createdList.getId()).orElseThrow();
            assertEquals("Lista Renomeada Persistida", updatedList.getName());
        }
    }

    @Nested
    @DisplayName("DELETE /api/lists/{id} - Excluir Lista")
    class DeleteListTests {

        @Test
        @DisplayName("Deve retornar 204 No Content quando exclui lista com sucesso")
        void shouldReturn204WhenDeleteListSuccessfully() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista para Excluir", 1), testUser);

            // Act & Assert
            mockMvc.perform(delete("/api/lists/{id}", createdList.getId()))
                    .andExpect(status().isNoContent());

            // Verificar que a lista foi removida do banco
            assertFalse(listRepository.findById(createdList.getId()).isPresent());
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário não é dono da lista")
        void shouldReturn403WhenUserIsNotOwner() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Privada", 1), testUser);

            // Criar outro usuário
            User otherUser = userService.createUser(
                    "otheruser",
                    "other@example.com",
                    "hashedPassword",
                    "Other User",
                    AuthProvider.EMAIL
            );

            // Autenticar como outro usuário
            authenticateUser(otherUser);

            // Act & Assert
            mockMvc.perform(delete("/api/lists/{id}", createdList.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Acesso Negado"))
                    .andExpect(jsonPath("$.detail").value("Apenas o dono pode excluir esta lista"));

            // Verificar que a lista NÃO foi removida
            assertTrue(listRepository.findById(createdList.getId()).isPresent());
        }

        @Test
        @DisplayName("Deve retornar 404 quando lista não existe")
        void shouldReturn404WhenListDoesNotExist() throws Exception {
            // Arrange
            authenticateUser(testUser);
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(delete("/api/lists/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Lista Não Encontrada"))
                    .andExpect(jsonPath("$.detail").value("Lista não encontrada"));
        }

        @Test
        @DisplayName("Deve retornar 401 quando não autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista Teste", 1), testUser);

            // Limpar autenticação
            SecurityContextHolder.clearContext();

            // Act & Assert
            mockMvc.perform(delete("/api/lists/{id}", createdList.getId()))
                    .andExpect(status().isUnauthorized());

            // Verificar que a lista NÃO foi removida
            assertTrue(listRepository.findById(createdList.getId()).isPresent());
        }

        @Test
        @DisplayName("Deve remover lista do banco de dados após exclusão")
        void shouldRemoveListFromDatabaseAfterDeletion() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista para Excluir", 1), testUser);
            UUID listId = createdList.getId();

            // Verificar que existe antes
            assertTrue(listRepository.findById(listId).isPresent());

            // Act
            mockMvc.perform(delete("/api/lists/{id}", listId))
                    .andExpect(status().isNoContent());

            // Assert - verificar que não existe mais
            assertFalse(listRepository.findById(listId).isPresent());
            assertEquals(0, listRepository.count());
        }

        // NOTE: Teste de CASCADE para list_items será adicionado no Epic 3
        // quando a tabela list_items for criada via migration V4 ou superior

        @Test
        @DisplayName("Deve deletar members via CASCADE quando lista é excluída")
        void shouldCascadeDeleteMembersWhenListDeleted() throws Exception {
            // Arrange
            authenticateUser(testUser);
            List createdList = listService.createList(new CreateListRequest("Lista com Membros", 1), testUser);
            UUID listId = createdList.getId();

            // Criar outro usuário para ser membro
            User member1 = userService.createUser("member1", "member1@example.com", "pass", "Member 1", AuthProvider.EMAIL);
            User member2 = userService.createUser("member2", "member2@example.com", "pass", "Member 2", AuthProvider.EMAIL);

            // CRITICAL: Flush JPA entities para DB antes de INSERT via JDBC
            entityManager.flush();
            entityManager.clear();

            // Inserir members diretamente via SQL
            // Schema da V3: id, list_id, user_id, role, created_at, updated_at
            jdbcTemplate.update(
                    "INSERT INTO list_members (id, list_id, user_id, role, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    UUID.randomUUID(), listId, member1.getId(), "MEMBER"
            );
            jdbcTemplate.update(
                    "INSERT INTO list_members (id, list_id, user_id, role, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    UUID.randomUUID(), listId, member2.getId(), "MEMBER"
            );

            // Verificar que members existem
            Integer membersCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM list_members WHERE list_id = ?",
                    Integer.class,
                    listId
            );
            assertEquals(2, membersCount, "Deve ter 2 members antes da exclusão");

            // Act - DELETE lista
            mockMvc.perform(delete("/api/lists/{id}", listId))
                    .andExpect(status().isNoContent());

            // CRITICAL: Flush após DELETE para garantir persistência
            entityManager.flush();
            entityManager.clear();

            // Assert - verificar que lista foi deletada
            assertFalse(listRepository.findById(listId).isPresent(), "Lista deve ser deletada");

            // Assert - verificar CASCADE: members foram deletados
            Integer membersCountAfter = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM list_members WHERE list_id = ?",
                    Integer.class,
                    listId
            );
            assertEquals(0, membersCountAfter, "Members devem ser deletados via CASCADE");
        }

        // NOTE: Teste combinado de CASCADE (items + members) será adicionado no Epic 3
        // quando ambas as tabelas estiverem disponíveis
    }
}
