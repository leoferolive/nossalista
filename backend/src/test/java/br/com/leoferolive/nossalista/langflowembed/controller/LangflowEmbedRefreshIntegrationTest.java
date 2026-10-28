package br.com.leoferolive.nossalista.langflowembed.controller;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.langflowembed.LangflowEmbedTestKeys;
import br.com.leoferolive.nossalista.langflowembed.dto.LangflowJwksResponse;
import br.com.leoferolive.nossalista.langflowembed.dto.LangflowRsaJwk;
import br.com.leoferolive.nossalista.langflowembed.service.LangflowEmbedKeyService;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthJwtService;
import br.com.leoferolive.nossalista.support.SessionCookieRequestPostProcessor;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Refresh do cookie do Langflow com {@code enabled=true}: 204 com cookie novo, sem redirect,
 * e as mesmas regras de autorização do launch.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "app.langflow-embed.enabled=true",
    "app.langflow-embed.issuer=https://nossalista-test.example.com.br"
})
@ActiveProfiles("test")
@Transactional
class LangflowEmbedRefreshIntegrationTest {

    private static final KeyPair KEYS = LangflowEmbedTestKeys.generate(2048);
    private static final String ISSUER = "https://nossalista-test.example.com.br";
    private static final String LANGFLOW_COOKIE = "nl_lf_token";

    @DynamicPropertySource
    static void langflowPrivateKey(DynamicPropertyRegistry registry) {
        registry.add("app.langflow-embed.private-key-pem", () -> LangflowEmbedTestKeys.pkcs8Pem(KEYS.getPrivate()));
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PersonalAccessTokenService personalAccessTokenService;

    @Autowired
    private McpOAuthJwtService mcpOAuthJwtService;

    @Autowired
    private McpOAuthProperties mcpOAuthProperties;

    @Autowired
    private LangflowEmbedKeyService keyService;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
        SecurityContextHolder.clearContext();
        user = userService.createUser(
            "ana.refresh", "ana.refresh@example.com", "hashed", "Ana Lista", AuthProvider.EMAIL);
    }

    @Test
    @DisplayName("refresh com sessão responde 204 sem corpo, sem Location e com Cache-Control no-store")
    void refreshWithSessionReturns204WithoutBodyOrRedirect() throws Exception {
        mockMvc.perform(get("/api/langflow/refresh")
                .with(SessionCookieRequestPostProcessor.session(jwtService.generateToken(user))))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    @DisplayName("refresh grava nl_lf_token com os mesmos atributos do launch")
    void refreshSetsLaunchCookieWithSameAttributes() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/langflow/refresh")
                .with(SessionCookieRequestPostProcessor.session(jwtService.generateToken(user))))
            .andExpect(status().isNoContent())
            .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
            .startsWith(LANGFLOW_COOKIE + "=")
            .contains("Path=/langflow", "Max-Age=900", "HttpOnly", "SameSite=Lax", "Secure")
            .doesNotContain("Domain=");
    }

    @Test
    @DisplayName("token do refresh valida contra o JWKS publicado e identifica o usuário")
    void refreshedTokenVerifiesAgainstPublishedJwks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/langflow/refresh")
                .with(SessionCookieRequestPostProcessor.session(jwtService.generateToken(user))))
            .andExpect(status().isNoContent())
            .andReturn();

        Claims claims = Jwts.parser()
            .verifyWith(publishedPublicKey())
            .requireIssuer(ISSUER)
            .requireAudience("langflow")
            .build()
            .parseSignedClaims(cookieValue(result))
            .getPayload();
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
    }

    @Test
    @DisplayName("refresh sem sessão retorna 401")
    void refreshWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/langflow/refresh"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh com PAT de escopo READ retorna 403: PAT não estende sessão no Langflow")
    void refreshWithPatReturns403() throws Exception {
        String pat = personalAccessTokenService.create(user.getId(),
            new CreatePersonalAccessTokenRequest("Refresh Token", TokenScope.READ, null)).token();

        mockMvc.perform(get("/api/langflow/refresh").header("Authorization", "Bearer " + pat))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("refresh com token OAuth do MCP retorna 401: o filtro do MCP só atua em /mcp")
    void refreshWithMcpOAuthTokenReturns401() throws Exception {
        String mcpToken = mcpOAuthJwtService.issueAccessToken(
            user.getId(), "claude-ai", TokenScope.READ_WRITE, mcpOAuthProperties.getResource());

        mockMvc.perform(get("/api/langflow/refresh").header("Authorization", "Bearer " + mcpToken))
            .andExpect(status().isUnauthorized());
    }

    private RSAPublicKey publishedPublicKey() {
        LangflowJwksResponse jwks = keyService.jwks();
        LangflowRsaJwk jwk = jwks.keys().get(0);
        return LangflowEmbedTestKeys.publicKeyFromJwk(jwk.n(), jwk.e());
    }

    /**
     * Devolve só o valor do cookie {@code nl_lf_token}, sem atributos.
     */
    private static String cookieValue(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }
}
