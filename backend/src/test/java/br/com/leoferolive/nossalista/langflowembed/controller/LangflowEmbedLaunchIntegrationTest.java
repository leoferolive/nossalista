package br.com.leoferolive.nossalista.langflowembed.controller;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.langflowembed.LangflowEmbedTestKeys;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthJwtService;
import br.com.leoferolive.nossalista.support.SessionCookieRequestPostProcessor;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Embed do Langflow com {@code enabled=true}: launch, cookie e JWKS de ponta a ponta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "app.langflow-embed.enabled=true",
    "app.langflow-embed.issuer=https://nossalista-test.example.com.br"
})
@ActiveProfiles("test")
@Transactional
class LangflowEmbedLaunchIntegrationTest {

    private static final KeyPair KEYS = LangflowEmbedTestKeys.generate(2048);
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
            "ana.embed", "ana.embed@example.com", "hashed", "Ana Lista", AuthProvider.EMAIL);
    }

    @Test
    @DisplayName("launch sem sessão retorna 401")
    void launchWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/langflow/launch"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("launch com PAT de escopo READ retorna 403: PAT não abre sessão no Langflow")
    void launchWithPatReturns403() throws Exception {
        String pat = personalAccessTokenService.create(user.getId(),
            new CreatePersonalAccessTokenRequest("Launch Token", TokenScope.READ, null)).token();

        mockMvc.perform(get("/api/langflow/launch").header("Authorization", "Bearer " + pat))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("launch com sessão responde 302 para /langflow/ e grava o cookie do embed")
    void launchWithSessionRedirectsAndSetsCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/langflow/launch")
                .with(SessionCookieRequestPostProcessor.session(jwtService.generateToken(user))))
            .andExpect(status().isFound())
            .andExpect(header().string(HttpHeaders.LOCATION, "/langflow/"))
            .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
            .startsWith(LANGFLOW_COOKIE + "=")
            .contains("Path=/langflow", "Max-Age=900", "HttpOnly", "SameSite=Lax", "Secure")
            .doesNotContain("Domain=");
    }

    @Test
    @DisplayName("token do cookie valida contra o JWKS publicado e identifica o usuário")
    void cookieTokenVerifiesAgainstPublishedJwks() throws Exception {
        String token = launchAndReadToken();

        String jwksBody = mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        RSAPublicKey publicKey = LangflowEmbedTestKeys.publicKeyFromJwk(
            JsonPath.read(jwksBody, "$.keys[0].n"), JsonPath.read(jwksBody, "$.keys[0].e"));

        var claims = Jwts.parser()
            .verifyWith(publicKey)
            .requireIssuer("https://nossalista-test.example.com.br")
            .requireAudience("langflow")
            .build()
            .parseSignedClaims(token)
            .getPayload();
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("preferred_username", String.class)).isEqualTo("ana.embed");
    }

    @Test
    @DisplayName("token do launch no cookie de sessão não autentica no NossaLista (assinatura RS256 rejeitada)")
    void launchTokenInSessionCookieDoesNotAuthenticate() throws Exception {
        String token = launchAndReadToken();

        mockMvc.perform(get("/api/users/me")
                .with(SessionCookieRequestPostProcessor.session(token)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token OAuth do MCP no launch retorna 401: o filtro do MCP só atua em /mcp")
    void launchWithMcpOAuthTokenReturns401() throws Exception {
        String mcpToken = mcpOAuthJwtService.issueAccessToken(
            user.getId(), "claude-ai", TokenScope.READ_WRITE, mcpOAuthProperties.getResource());

        mockMvc.perform(get("/api/langflow/launch").header("Authorization", "Bearer " + mcpToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bearer que não é PAT no launch retorna 401, mesmo um JWT RS256 válido do embed")
    void launchWithNonPatBearerReturns401() throws Exception {
        String launchToken = launchAndReadToken();

        mockMvc.perform(get("/api/langflow/launch").header("Authorization", "Bearer " + launchToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout com embed ligado expira nl_lf_token com os atributos do launch")
    void logoutExpiresLaunchCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                .with(SessionCookieRequestPostProcessor.session(jwtService.generateToken(user))))
            .andExpect(status().isNoContent())
            .andReturn();

        String launchCookie = setCookieNamed(result, LANGFLOW_COOKIE);
        assertThat(launchCookie)
            .startsWith(LANGFLOW_COOKIE + "=")
            .contains("Path=/langflow", "Max-Age=0", "HttpOnly", "SameSite=Lax", "Secure")
            .doesNotContain("Domain=");
    }

    @Test
    @DisplayName("JWKS é público e publica uma chave RS256 de assinatura com kid")
    void jwksIsPublicAndAdvertisesRs256Key() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys[0].kid").value("nl-langflow-1"))
            .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
            .andExpect(jsonPath("$.keys[0].use").value("sig"))
            .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
    }

    private static String setCookieNamed(MvcResult result, String name) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
            .filter(header -> header.startsWith(name + "="))
            .findFirst()
            .orElse("");
    }

    /**
     * Faz o launch com sessão e devolve só o valor do cookie do Langflow, sem atributos.
     */
    private String launchAndReadToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/langflow/launch")
                .with(SessionCookieRequestPostProcessor.session(jwtService.generateToken(user))))
            .andExpect(status().isFound())
            .andReturn();
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }
}
