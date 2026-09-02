package br.com.leoferolive.nossalista.apitoken.controller;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.repository.PersonalAccessTokenRepository;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static br.com.leoferolive.nossalista.support.SessionCookieRequestPostProcessor.session;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do CRUD de Personal Access Tokens via cookie de sessão
 * HttpOnly (fluxo esperado pela UI de gestão de tokens).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class PersonalAccessTokenControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PersonalAccessTokenRepository tokenRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User userA;
    private String bearerA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
        objectMapper = new ObjectMapper();
        SecurityContextHolder.clearContext();

        userA = userService.createUser(
            "tokenowner", "tokenowner@example.com", "hashed", "Token Owner", AuthProvider.EMAIL);
        bearerA = "Bearer " + jwtService.generateToken(userA);
    }

    private String createTokenPayload(String name, TokenScope scope, Integer expiresInDays) throws Exception {
        return objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("name", name);
            put("scope", scope.name());
            if (expiresInDays != null) {
                put("expiresInDays", expiresInDays);
            }
        }});
    }

    @Test
    @DisplayName("POST cria token e retorna o valor em claro uma única vez")
    void createTokenReturnsPlainTokenOnce() throws Exception {
        mockMvc.perform(post("/api/users/me/tokens")
                .with(session(bearerA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTokenPayload("Claude Desktop", TokenScope.READ, null)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token", org.hamcrest.Matchers.startsWith("nlmcp_")))
            .andExpect(jsonPath("$.name").value("Claude Desktop"))
            .andExpect(jsonPath("$.scope").value("READ"))
            .andExpect(jsonPath("$.prefix", org.hamcrest.Matchers.startsWith("nlmcp_")));
    }

    @Test
    @DisplayName("POST com dados inválidos retorna 400")
    void createTokenWithInvalidDataReturns400() throws Exception {
        mockMvc.perform(post("/api/users/me/tokens")
                .with(session(bearerA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"scope\":\"READ\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST além do limite de tokens ativos retorna 409")
    void createTokenBeyondLimitReturns409() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/users/me/tokens")
                    .with(session(bearerA))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createTokenPayload("Token " + i, TokenScope.READ, null)))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/users/me/tokens")
                .with(session(bearerA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTokenPayload("Token 11", TokenScope.READ, null)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET lista tokens sem expor token ou hash")
    void listTokensNeverExposesTokenOrHash() throws Exception {
        mockMvc.perform(post("/api/users/me/tokens")
                .with(session(bearerA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTokenPayload("Token Listado", TokenScope.READ_WRITE, 90)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/me/tokens").with(session(bearerA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Token Listado"))
            .andExpect(jsonPath("$[0].scope").value("READ_WRITE"))
            .andExpect(jsonPath("$[0].token").doesNotExist())
            .andExpect(jsonPath("$[0].tokenHash").doesNotExist());
    }

    @Test
    @DisplayName("GET não retorna tokens revogados")
    void listDoesNotIncludeRevokedTokens() throws Exception {
        String body = mockMvc.perform(post("/api/users/me/tokens")
                .with(session(bearerA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTokenPayload("A ser revogado", TokenScope.READ, null)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(delete("/api/users/me/tokens/{id}", id).with(session(bearerA)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/tokens").with(session(bearerA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("GET não retorna tokens de outros usuários")
    void listDoesNotReturnOtherUsersTokens() throws Exception {
        mockMvc.perform(post("/api/users/me/tokens")
                .with(session(bearerA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTokenPayload("Da Usuária A", TokenScope.READ, null)))
            .andExpect(status().isCreated());

        User userB = userService.createUser(
            "tokenlistintruder", "listintruder@example.com", "hashed", "List Intruder", AuthProvider.EMAIL);
        String bearerB = "Bearer " + jwtService.generateToken(userB);

        mockMvc.perform(get("/api/users/me/tokens").with(session(bearerB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("DELETE revoga o token do próprio usuário")
    void revokeOwnTokenSucceeds() throws Exception {
        String body = mockMvc.perform(post("/api/users/me/tokens")
                .with(session(bearerA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTokenPayload("A revogar", TokenScope.READ, null)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(delete("/api/users/me/tokens/{id}", id).with(session(bearerA)))
            .andExpect(status().isNoContent());

        assertRevoked(id);
    }

    @Test
    @DisplayName("DELETE é idempotente: revogar duas vezes não é erro")
    void revokeIsIdempotentOverHttp() throws Exception {
        String body = mockMvc.perform(post("/api/users/me/tokens")
                .with(session(bearerA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTokenPayload("Duas vezes", TokenScope.READ, null)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(delete("/api/users/me/tokens/{id}", id).with(session(bearerA)))
            .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/users/me/tokens/{id}", id).with(session(bearerA)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE de token de outro usuário retorna 404")
    void revokeOtherUsersTokenReturns404() throws Exception {
        String body = mockMvc.perform(post("/api/users/me/tokens")
                .with(session(bearerA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTokenPayload("Da Usuária A", TokenScope.READ, null)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        User userB = userService.createUser(
            "tokenintruder", "intruder@example.com", "hashed", "Intruder", AuthProvider.EMAIL);
        String bearerB = "Bearer " + jwtService.generateToken(userB);

        mockMvc.perform(delete("/api/users/me/tokens/{id}", id).with(session(bearerB)))
            .andExpect(status().isNotFound());

        assertThatTokenIsStillActive(id);
    }

    @Test
    @DisplayName("DELETE de id inexistente retorna 404")
    void revokeUnknownIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/users/me/tokens/{id}", UUID.randomUUID()).with(session(bearerA)))
            .andExpect(status().isNotFound());
    }

    private void assertRevoked(UUID id) {
        var token = tokenRepository.findById(id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(token.getRevokedAt()).isNotNull();
    }

    private void assertThatTokenIsStillActive(UUID id) {
        var token = tokenRepository.findById(id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(token.getRevokedAt()).isNull();
    }
}
