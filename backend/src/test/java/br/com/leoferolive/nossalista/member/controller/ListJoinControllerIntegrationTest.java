package br.com.leoferolive.nossalista.member.controller;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.support.RegressionTest;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para ListJoinController
 * Testa endpoint público de visualização de lista via convite (read-only)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@RegressionTest
@DisplayName("ListJoinController Integration Tests")
class ListJoinControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private ListRepository listRepository;

    @Autowired
    private ListItemRepository listItemRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private User testOwner;
    private List testList;
    private static final String VALID_INVITE_CODE = "ABC123XYZ789";

    private String bearerToken(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        // Create test owner
        testOwner = userService.createUser(
            "mariana",
            "mariana@example.com",
            "hashedPassword",
            "Mariana Silva",
            AuthProvider.EMAIL
        );
        testOwner.setAvatarUrl("https://example.com/avatar.jpg");

        // Create test list with valid invite code
        testList = new List();
        testList.setId(UUID.randomUUID());
        testList.setName("Mercado Semanal");
        testList.setTypeId(1); // Compras
        testList.setOwner(testOwner);
        testList.setInviteCode(VALID_INVITE_CODE);
        testList.setInviteExpiresAt(LocalDateTime.now().plusHours(24));
        listRepository.save(testList);
    }

    @Test
    @DisplayName("Deve retornar 200 com dados da lista quando invite code é válido")
    void shouldReturn200WithListDataWhenInviteCodeValid() throws Exception {
        mockMvc.perform(get("/api/lists/join/" + VALID_INVITE_CODE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Mercado Semanal"))
            .andExpect(jsonPath("$.mode").value("READ_ONLY"))
            .andExpect(jsonPath("$.inviteCode").value(VALID_INVITE_CODE))
            .andExpect(jsonPath("$.typeSlug").value("compras"))
            .andExpect(jsonPath("$.typeName").value("Compras"))
            .andExpect(jsonPath("$.ownerUsername").value("mariana"))
            .andExpect(jsonPath("$.ownerName").value("Mariana Silva"));
    }

    @Test
    @DisplayName("Deve retornar 404 quando invite code não existe")
    void shouldReturn404WhenInviteCodeNotFound() throws Exception {
        mockMvc.perform(get("/api/lists/join/INVALIDCODE123"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @DisplayName("Deve retornar 410 quando invite code expirou")
    void shouldReturn410WhenInviteCodeExpired() throws Exception {
        // Update list to have expired invite
        testList.setInviteExpiresAt(LocalDateTime.now().minusHours(1));
        listRepository.save(testList);

        mockMvc.perform(get("/api/lists/join/" + VALID_INVITE_CODE))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.title").value(containsString("expirado")));
    }

    @Test
    @DisplayName("Deve retornar 410 quando expires_at é nulo")
    void shouldReturn410WhenExpiresAtIsNull() throws Exception {
        // Update list to have null expiration
        testList.setInviteExpiresAt(null);
        listRepository.save(testList);

        mockMvc.perform(get("/api/lists/join/" + VALID_INVITE_CODE))
            .andExpect(status().isGone());
    }

    @Test
    @DisplayName("Deve retornar 200 sem autenticação (endpoint público)")
    void shouldReturn200WithoutAuthentication() throws Exception {
        // No Authorization header - endpoint is public
        mockMvc.perform(get("/api/lists/join/" + VALID_INVITE_CODE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Deve retornar itens em modo read-only ordenados por position")
    void shouldReturnItemsInReadOnlyModeOrderedByPosition() throws Exception {
        // Create items in different positions
        ListItem item1 = createListItem("Feijão", 1, 1);
        ListItem item2 = createListItem("Arroz", 0, 2); // Should be first
        ListItem item3 = createListItem("Leite", 2, 3);
        listItemRepository.save(item1);
        listItemRepository.save(item2);
        listItemRepository.save(item3);

        mockMvc.perform(get("/api/lists/join/" + VALID_INVITE_CODE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("READ_ONLY"))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items", hasSize(3)))
            .andExpect(jsonPath("$.items[0].name").value("Arroz"))
            .andExpect(jsonPath("$.items[0].position").value(0))
            .andExpect(jsonPath("$.items[1].name").value("Feijão"))
            .andExpect(jsonPath("$.items[1].position").value(1))
            .andExpect(jsonPath("$.items[2].name").value("Leite"))
            .andExpect(jsonPath("$.items[2].position").value(2));
    }

    @Test
    @DisplayName("Deve retornar dados dos itens corretamente")
    void shouldReturnItemDataCorrectly() throws Exception {
        ListItem item = createListItemWithAllFields("Arroz", 0, 2, false);
        listItemRepository.save(item);

        mockMvc.perform(get("/api/lists/join/" + VALID_INVITE_CODE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].name").value("Arroz"))
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.items[0].checked").value(false))
            .andExpect(jsonPath("$.items[0].position").value(0));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há itens")
    void shouldReturnEmptyListWhenNoItems() throws Exception {
        mockMvc.perform(get("/api/lists/join/" + VALID_INVITE_CODE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("Deve retornar expires_at no formato correto")
    void shouldReturnExpiresAtInCorrectFormat() throws Exception {
        mockMvc.perform(get("/api/lists/join/" + VALID_INVITE_CODE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expiresAt").exists());
    }

    private ListItem createListItem(String name, int position, int quantity) {
        ListItem item = new ListItem();
        item.setId(UUID.randomUUID());
        item.setName(name);
        item.setPosition(position);
        item.setQuantity(quantity);
        item.setChecked(false);
        item.setList(testList);
        item.setCreatedBy(testOwner);
        return item;
    }

    private ListItem createListItemWithAllFields(String name, int position, int quantity, boolean checked) {
        ListItem item = new ListItem();
        item.setId(UUID.randomUUID());
        item.setName(name);
        item.setPosition(position);
        item.setQuantity(quantity);
        item.setChecked(checked);
        item.setList(testList);
        item.setCreatedBy(testOwner);
        return item;
    }

    // ==================== TESTES PARA POST /join/{inviteCode} ====================

    @Test
    @DisplayName("POST join: Deve retornar 201 quando usuário entra como novo membro")
    void shouldReturn201WhenUserJoinsAsNewMember() throws Exception {
        // Create a new user (not owner)
        User newUser = userService.createUser(
            "pedro",
            "pedro@example.com",
            "hashedPassword",
            "Pedro Santos",
            AuthProvider.EMAIL
        );

        mockMvc.perform(post("/api/lists/join/" + VALID_INVITE_CODE)
                .header("Authorization", bearerToken(newUser)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(testList.getId().toString()))
            .andExpect(jsonPath("$.name").value("Mercado Semanal"))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.message").value("Bem-vindo à lista Mercado Semanal!"))
            .andExpect(jsonPath("$.typeSlug").value("compras"))
            .andExpect(jsonPath("$.list").exists());
    }

    @Test
    @DisplayName("POST join: Deve retornar 200 quando usuário já é membro")
    void shouldReturn200WhenUserAlreadyMember() throws Exception {
        // Create a new user
        User newUser = userService.createUser(
            "pedro",
            "pedro@example.com",
            "hashedPassword",
            "Pedro Santos",
            AuthProvider.EMAIL
        );

        // First join - creates member
        mockMvc.perform(post("/api/lists/join/" + VALID_INVITE_CODE)
                .header("Authorization", bearerToken(newUser)))
            .andExpect(status().isCreated());

        // Second join - should return 200 OK
        mockMvc.perform(post("/api/lists/join/" + VALID_INVITE_CODE)
                .header("Authorization", bearerToken(newUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.message").value("Você já é membro desta lista"));
    }

    @Test
    @DisplayName("POST join: Deve retornar 200 quando usuário é o dono")
    void shouldReturn200WhenUserIsOwner() throws Exception {
        mockMvc.perform(post("/api/lists/join/" + VALID_INVITE_CODE)
                .header("Authorization", bearerToken(testOwner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("OWNER"))
            .andExpect(jsonPath("$.message").value("Você é o dono desta lista"));
    }

    @Test
    @DisplayName("POST join: Deve retornar 401 sem autenticação")
    void shouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/lists/join/" + VALID_INVITE_CODE))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST join: Deve retornar 404 quando invite code não existe")
    void shouldReturn404WhenInviteCodeNotFoundForPost() throws Exception {
        User newUser = userService.createUser(
            "pedro",
            "pedro@example.com",
            "hashedPassword",
            "Pedro Santos",
            AuthProvider.EMAIL
        );

        mockMvc.perform(post("/api/lists/join/INVALIDCODE")
                .header("Authorization", bearerToken(newUser)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @DisplayName("POST join: Deve retornar 410 quando invite code expirou")
    void shouldReturn410WhenInviteCodeExpiredForPost() throws Exception {
        // Update list to have expired invite
        testList.setInviteExpiresAt(LocalDateTime.now().minusHours(1));
        listRepository.save(testList);

        User newUser = userService.createUser(
            "pedro",
            "pedro@example.com",
            "hashedPassword",
            "Pedro Santos",
            AuthProvider.EMAIL
        );

        mockMvc.perform(post("/api/lists/join/" + VALID_INVITE_CODE)
                .header("Authorization", bearerToken(newUser)))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.title").value(containsString("expirado")));
    }
}
