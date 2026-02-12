package br.com.leoferolive.nossalista.user.controller;

import br.com.leoferolive.nossalista.auth.dto.UserMapper;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.dto.UpdateProfileRequest;
import br.com.leoferolive.nossalista.user.dto.UserProfileResponse;
import br.com.leoferolive.nossalista.user.dto.UserSearchResponse;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import br.com.leoferolive.nossalista.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para UserController
 * Cobrem: GET /api/users/me, PATCH /api/users/me, GET /api/users/search
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EntityManager entityManager;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    private User testUser;
    private User anotherUser;

    /**
     * Configura MockMvc e cria usuários de teste
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())  // Aplica filtros de segurança do Spring Security
            .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Registra JavaTimeModule para LocalDateTime

        // Limpar SecurityContext antes de cada teste
        SecurityContextHolder.clearContext();

        // Criar usuário de teste
        testUser = userService.createUser(
            "testuser",
            "test@example.com",
            "hashedPassword",
            "Test User",
            AuthProvider.EMAIL
        );

        // Criar outro usuário para busca
        anotherUser = userService.createUser(
            "leonardo",
            "leo@example.com",
            "hashedPassword",
            "Leonardo Oliveira",
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
    @DisplayName("GET /api/users/me - Obter Próprio Perfil")
    class GetMyProfileTests {

        @Test
        @DisplayName("Deve retornar perfil completo com JWT válido")
        void shouldReturnCompleteProfileWithValidToken() throws Exception {
            // Given - usuário autenticado no SecurityContext
            authenticateUser(testUser);

            // When - chamar GET /api/users/me
            MvcResult result = mockMvc.perform(get("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.name").value(testUser.getName()))
                .andExpect(jsonPath("$.avatarUrl").value(testUser.getAvatarUrl()))
                .andExpect(jsonPath("$.authProvider").value(testUser.getAuthProvider().toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                // Verifica que password NÃO está no response
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

            // Then - response contém todos os campos esperados
            String responseJson = result.getResponse().getContentAsString();
            UserProfileResponse response = objectMapper.readValue(responseJson, UserProfileResponse.class);

            assertEquals(testUser.getId(), response.id());
            assertEquals(testUser.getUsername(), response.username());
            assertEquals(testUser.getEmail(), response.email());
            assertEquals(testUser.getName(), response.name());
            assertEquals(testUser.getAvatarUrl(), response.avatarUrl());
            assertEquals(testUser.getAuthProvider().toString(), response.authProvider());
        }

        @Test
        @DisplayName("Deve retornar 401 sem autenticação")
        void shouldReturn401WithoutAuthentication() throws Exception {
            // Given - SecurityContext vazio (sem autenticação)
            SecurityContextHolder.clearContext();

            // When - chamar GET /api/users/me sem token
            mockMvc.perform(get("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON))
                // Then - deve retornar 401 Unauthorized
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.status").value(401));
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me - Atualizar Próprio Perfil")
    class UpdateProfileTests {

        @Test
        @DisplayName("Deve atualizar name com sucesso")
        void shouldUpdateNameSuccessfully() throws Exception {
            // Given - usuário autenticado
            authenticateUser(testUser);

            UpdateProfileRequest request = new UpdateProfileRequest("New Name", null);

            // When - chamar PATCH /api/users/me
            mockMvc.perform(patch("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                // Then - deve retornar 200 com dados atualizados
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.username").value(testUser.getUsername())) // não alterado
                .andExpect(jsonPath("$.email").value(testUser.getEmail())); // não alterado
        }

        @Test
        @DisplayName("Deve atualizar avatarUrl com sucesso")
        void shouldUpdateAvatarUrlSuccessfully() throws Exception {
            // Given - usuário autenticado
            authenticateUser(testUser);

            UpdateProfileRequest request = new UpdateProfileRequest(null, "https://example.com/new-avatar.jpg");

            // When - chamar PATCH /api/users/me
            mockMvc.perform(patch("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                // Then - deve retornar 200 com avatar atualizado
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/new-avatar.jpg"))
                .andExpect(jsonPath("$.name").value(testUser.getName())) // não alterado
                .andExpect(jsonPath("$.username").value(testUser.getUsername())) // não alterado
                .andExpect(jsonPath("$.email").value(testUser.getEmail())); // não alterado
        }

        @Test
        @DisplayName("Deve atualizar updatedAt automaticamente via @PreUpdate")
        void shouldUpdateUpdatedAtAutomatically() throws Exception {
            // Given - usuário autenticado
            authenticateUser(testUser);

            // Flush e clear para garantir estado sincronizado com banco
            entityManager.flush();
            entityManager.clear();

            // Recarregar usuário do banco para garantir estado fresh
            User freshUser = userRepository.findById(testUser.getId()).orElseThrow();
            LocalDateTime originalUpdatedAt = freshUser.getUpdatedAt();

            // Sleep para garantir timestamp diferente (1 segundo para garantia)
            Thread.sleep(1000);

            UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", null);

            // When - chamar PATCH /api/users/me
            mockMvc.perform(patch("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // Flush e clear novamente para forçar sincronização
            entityManager.flush();
            entityManager.clear();

            // Then - updatedAt deve ser maior que o original
            User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
            assertTrue(updatedUser.getUpdatedAt().isAfter(originalUpdatedAt),
                "updatedAt deve ser atualizado automaticamente via @PreUpdate. " +
                "Original: " + originalUpdatedAt + ", Updated: " + updatedUser.getUpdatedAt());
            assertEquals(freshUser.getCreatedAt(), updatedUser.getCreatedAt(),
                "createdAt não deve ser alterado");
        }

        @Test
        @DisplayName("Deve retornar 401 sem autenticação")
        void shouldReturn401WithoutAuthentication() throws Exception {
            // Given - SecurityContext vazio
            SecurityContextHolder.clearContext();

            UpdateProfileRequest request = new UpdateProfileRequest("New Name", null);

            // When - chamar PATCH /api/users/me sem token
            mockMvc.perform(patch("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                // Then - deve retornar 401
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/users/search - Buscar Usuários")
    class SearchUsersTests {

        @Test
        @DisplayName("Deve buscar usuários com termo válido")
        void shouldSearchUsersWithValidQuery() throws Exception {
            // Given - usuário autenticado
            authenticateUser(testUser);

            // When - buscar por "leo"
            mockMvc.perform(get("/api/users/search")
                    .param("q", "leo")
                    .contentType(MediaType.APPLICATION_JSON))
                // Then - deve retornar 200 com array de usuários
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value("leonardo"))
                .andExpect(jsonPath("$[0].name").value("Leonardo Oliveira"))
                // Verifica que email e password NÃO estão no response
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
        }

        @Test
        @DisplayName("Deve buscar case-insensitive")
        void shouldSearchCaseInsensitive() throws Exception {
            // Given - usuário autenticado
            authenticateUser(testUser);

            // When - buscar por "LEO" (maiúsculo)
            mockMvc.perform(get("/api/users/search")
                    .param("q", "LEO")
                    .contentType(MediaType.APPLICATION_JSON))
                // Then - deve encontrar "leonardo" (minúsculo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value("leonardo"));
        }

        @Test
        @DisplayName("Deve buscar parcial")
        void shouldSearchPartialMatch() throws Exception {
            // Given - usuário autenticado
            authenticateUser(testUser);

            // When - buscar por "leon" (parcial)
            mockMvc.perform(get("/api/users/search")
                    .param("q", "leon")
                    .contentType(MediaType.APPLICATION_JSON))
                // Then - deve encontrar "leonardo"
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value("leonardo"));
        }

        @Test
        @DisplayName("Deve retornar array vazio quando não encontra resultados")
        void shouldReturnEmptyArrayWhenNoResults() throws Exception {
            // Given - usuário autenticado
            authenticateUser(testUser);

            // When - buscar por termo que não existe
            mockMvc.perform(get("/api/users/search")
                    .param("q", "nonexistent")
                    .contentType(MediaType.APPLICATION_JSON))
                // Then - deve retornar 200 com array vazio
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Deve retornar 400 quando query está ausente")
        void shouldReturn400WhenQueryMissing() throws Exception {
            // Given - usuário autenticado
            authenticateUser(testUser);

            // When - chamar sem query parameter
            mockMvc.perform(get("/api/users/search")
                    .contentType(MediaType.APPLICATION_JSON))
                // Then - deve retornar 400 Bad Request
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 400 quando query tem menos de 2 caracteres")
        void shouldReturn400WhenQueryTooShort() throws Exception {
            // Given - usuário autenticado
            authenticateUser(testUser);

            // When - buscar com 1 caractere apenas
            mockMvc.perform(get("/api/users/search")
                    .param("q", "a")
                    .contentType(MediaType.APPLICATION_JSON))
                // Then - deve retornar 400
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Query deve ter no mínimo 2 caracteres"));
        }

        @Test
        @DisplayName("Deve retornar 401 sem autenticação")
        void shouldReturn401WithoutAuthentication() throws Exception {
            // Given - SecurityContext vazio
            SecurityContextHolder.clearContext();

            // When - buscar sem autenticação
            mockMvc.perform(get("/api/users/search")
                    .param("q", "leo")
                    .contentType(MediaType.APPLICATION_JSON))
                // Then - deve retornar 401
                .andExpect(status().isUnauthorized());
        }
    }
}
