package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.repository.PersonalAccessTokenRepository;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração ponta-a-ponta da autenticação via Personal Access
 * Token (PAT) contra endpoints reais — cobre o caminho pelo qual um cliente
 * MCP/API externo efetivamente vai autenticar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class PersonalAccessTokenAuthorizationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private PersonalAccessTokenService tokenService;

    @Autowired
    private PersonalAccessTokenRepository tokenRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
        SecurityContextHolder.clearContext();
        rateLimiterService.reset();

        user = userService.createUser(
            "mcpclient", "mcpclient@example.com", "hashed", "MCP Client", AuthProvider.EMAIL);
    }

    private String issueToken(TokenScope scope) {
        var response = tokenService.create(user.getId(),
            new CreatePersonalAccessTokenRequest("Integration Token", scope, null));
        return response.token();
    }

    @Test
    @DisplayName("PAT válido autentica em endpoint real de listas (GET)")
    void validPatAuthenticatesRealListEndpoint() throws Exception {
        String token = issueToken(TokenScope.READ);

        mockMvc.perform(get("/api/lists").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PAT com escopo READ pode fazer GET mas não POST (403)")
    void readScopeCannotMutate() throws Exception {
        String token = issueToken(TokenScope.READ);

        mockMvc.perform(post("/api/lists")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Lista via PAT\",\"typeId\":1}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PAT com escopo READ_WRITE pode fazer POST (mutação)")
    void readWriteScopeCanMutate() throws Exception {
        String token = issueToken(TokenScope.READ_WRITE);

        mockMvc.perform(post("/api/lists")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Lista via PAT\",\"typeId\":1}"))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PAT revogado retorna 401")
    void revokedPatReturns401() throws Exception {
        String token = issueToken(TokenScope.READ);
        var stored = tokenRepository.findAllByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(user.getId()).get(0);
        stored.setRevokedAt(LocalDateTime.now());
        tokenRepository.save(stored);

        mockMvc.perform(get("/api/lists").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PAT expirado retorna 401")
    void expiredPatReturns401() throws Exception {
        String token = issueToken(TokenScope.READ);
        var stored = tokenRepository.findAllByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(user.getId()).get(0);
        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(stored);

        mockMvc.perform(get("/api/lists").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PAT com valor inexistente retorna 401")
    void unknownPatReturns401() throws Exception {
        mockMvc.perform(get("/api/lists")
                .header("Authorization", "Bearer nlmcp_" + "0".repeat(64)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PAT (qualquer escopo) não pode gerenciar tokens: 403 em GET, POST e DELETE")
    void patCannotManageTokens() throws Exception {
        String readToken = issueToken(TokenScope.READ);
        String readWriteToken = issueToken(TokenScope.READ_WRITE);

        mockMvc.perform(get("/api/users/me/tokens").header("Authorization", "Bearer " + readToken))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/me/tokens").header("Authorization", "Bearer " + readWriteToken))
            .andExpect(status().isForbidden());

        // Regra é por path (não por método), mas assevera explicitamente POST e DELETE
        // para não depender apenas da inferência de que "por path" cobre todos os verbos.
        mockMvc.perform(post("/api/users/me/tokens")
                .header("Authorization", "Bearer " + readWriteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"via PAT\",\"scope\":\"READ\"}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/me/tokens/{id}", java.util.UUID.randomUUID())
                .header("Authorization", "Bearer " + readWriteToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PAT não pode acessar endpoints de autenticação: 403")
    void patCannotAccessAuthEndpoints() throws Exception {
        String token = issueToken(TokenScope.READ_WRITE);

        mockMvc.perform(get("/api/auth/verify-email")
                .param("token", "whatever")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("requisição anônima a /api/auth/register continua pública mesmo com a nova regra de PAT")
    void anonymousStillPublicOnAuthEndpoints() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x\",\"username\":\"x\",\"password\":\"x\"}"))
            .andExpect(status().isBadRequest()); // chega no controller (validação), não é bloqueado por auth
    }

    @Test
    @DisplayName("uso do PAT atualiza last_used_at")
    void patUsageUpdatesLastUsedAt() throws Exception {
        String token = issueToken(TokenScope.READ);
        UUID tokenId = tokenRepository.findAllByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(user.getId()).get(0).getId();

        mockMvc.perform(get("/api/lists").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        var stored = tokenRepository.findById(tokenId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(stored.getLastUsedAt()).isNotNull();
    }
}
