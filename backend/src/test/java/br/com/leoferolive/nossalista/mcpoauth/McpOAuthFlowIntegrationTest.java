package br.com.leoferolive.nossalista.mcpoauth;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.dto.ConsentDecisionResponse;
import br.com.leoferolive.nossalista.mcpoauth.dto.OAuthConnectionSummary;
import br.com.leoferolive.nossalista.mcpoauth.dto.PendingAuthorizationView;
import br.com.leoferolive.nossalista.mcpoauth.dto.TokenResponse;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthAuthorizationService;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthJwtService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de integração ponta-a-ponta do servidor de autorização OAuth 2.1 do
 * MCP (Fase C — docs/DECISIONS.md D-021): authorize → consentimento → code →
 * token → chamada real ao servidor MCP com o access token emitido.
 *
 * <p>Segue o mesmo padrão de isolamento de {@code McpServerIntegrationTest}
 * (Fase B): H2 dedicado via {@code @TestPropertySource}, para não poluir o
 * {@code testdb} compartilhado por outras suítes {@code @SpringBootTest}.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:mcp-oauth-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
class McpOAuthFlowIntegrationTest {

    private static final String LOCAL_CLIENT_ID = "nossalista-local";
    private static final String LOCAL_REDIRECT_URI = "http://localhost:8765/callback";
    private static final String CLAUDE_CODE_CLIENT_ID = "claude-code";

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private McpOAuthProperties properties;

    @Autowired
    private McpOAuthJwtService oauthJwtService;

    @Value("${frontend.url}")
    private String frontendUrl;

    private final List<McpSyncClient> openClients = new ArrayList<>();
    private final HttpClient noRedirectHttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    private final RestClient restClient = RestClient.builder()
        .requestFactory(new JdkClientHttpRequestFactory(noRedirectHttpClient))
        .build();

    @AfterEach
    void closeClients() {
        openClients.forEach(McpSyncClient::close);
        openClients.clear();
    }

    // ---------------------------------------------------------------- helpers

    private User newUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userService.createUser(
            "oauthuser" + suffix, "oauthuser" + suffix + "@example.com", "hashed", "OAuth User " + suffix,
            AuthProvider.EMAIL);
    }

    private String sessionJwt(User user) {
        return jwtService.generateToken(user);
    }

    private record Pkce(String verifier, String challenge) {
    }

    private Pkce newPkce() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Pkce(verifier, sha256Base64Url(verifier));
    }

    private String sha256Base64Url(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Extrai o cookie de vínculo de consentimento (ver
     * {@code McpOAuthAuthorizationService.CONSENT_COOKIE_NAME}) do header
     * {@code Set-Cookie} da resposta de {@code GET /oauth/authorize}, no formato
     * {@code name=value} pronto para reenviar num header {@code Cookie}.
     */
    private String extractConsentCookie(ResponseEntity<Void> authorizeResponse) {
        List<String> setCookies = authorizeResponse.getHeaders().get(org.springframework.http.HttpHeaders.SET_COOKIE);
        assertThat(setCookies).as("Set-Cookie presente na resposta de /oauth/authorize").isNotNull();
        return setCookies.stream()
            .filter(header -> header.startsWith(McpOAuthAuthorizationService.CONSENT_COOKIE_NAME + "="))
            .map(header -> {
                int semicolon = header.indexOf(';');
                return semicolon == -1 ? header : header.substring(0, semicolon);
            })
            .findFirst()
            .orElseThrow(() -> new AssertionError("Cookie de consentimento não encontrado em: " + setCookies));
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private URI authorizeUri(
        String clientId, String redirectUri, String scope, String state, String challenge, String method, String resource
    ) {
        String query = "response_type=code"
            + "&client_id=" + encode(clientId)
            + "&redirect_uri=" + encode(redirectUri)
            + "&scope=" + encode(scope)
            + "&state=" + encode(state)
            + "&code_challenge=" + encode(challenge)
            + "&code_challenge_method=" + encode(method)
            + "&resource=" + encode(resource);
        return URI.create(baseUrl() + "/oauth/authorize?" + query);
    }

    private ResponseEntity<Void> callAuthorize(URI uri) {
        return restClient.get().uri(uri).retrieve().toBodilessEntity();
    }

    /** Executa authorize (S256, resource canônico) + consentimento aprovado; devolve code+state extraídos. */
    private Map<String, String> fullyAuthorize(
        String sessionJwt, String clientId, String redirectUri, TokenScope scope, Pkce pkce
    ) {
        String state = "state-" + UUID.randomUUID();
        URI uri = authorizeUri(clientId, redirectUri, scope.name().toLowerCase(java.util.Locale.ROOT), state,
            pkce.challenge(), "S256", properties.getResource());
        ResponseEntity<Void> authorizeResponse = callAuthorize(uri);
        assertThat(authorizeResponse.getStatusCode().value()).isEqualTo(302);
        String location = authorizeResponse.getHeaders().getLocation().toString();
        assertThat(location).startsWith(frontendUrl + "/oauth/consent?request_id=");
        String consentCookie = extractConsentCookie(authorizeResponse);

        UUID requestId = UUID.fromString(location.substring(location.indexOf("request_id=") + "request_id=".length()));

        PendingAuthorizationView view = restClient.get()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId)
            .header("Authorization", "Bearer " + sessionJwt)
            .header("Cookie", consentCookie)
            .retrieve()
            .body(PendingAuthorizationView.class);
        assertThat(view.clientId()).isEqualTo(clientId);
        assertThat(view.scope()).isEqualTo(scope);

        ConsentDecisionResponse decision = restClient.post()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId + "/approve")
            .header("Authorization", "Bearer " + sessionJwt)
            .header("Cookie", consentCookie)
            .retrieve()
            .body(ConsentDecisionResponse.class);

        String redirectUrl = decision.redirectUrl();
        assertThat(redirectUrl).startsWith(redirectUri);
        String code = extractParam(redirectUrl, "code");
        String returnedState = extractParam(redirectUrl, "state");
        assertThat(returnedState).isEqualTo(state);

        return Map.of("code", code, "state", returnedState);
    }

    private String extractParam(String url, String name) {
        String marker = name + "=";
        int start = url.indexOf(marker);
        assertThat(start).as("param %s presente em %s", name, url).isGreaterThanOrEqualTo(0);
        int begin = start + marker.length();
        int end = url.indexOf('&', begin);
        String raw = end == -1 ? url.substring(begin) : url.substring(begin, end);
        return java.net.URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    private TokenResponse exchangeCode(String code, String redirectUri, String clientId, String codeVerifier, String resource) {
        String form = "grant_type=authorization_code"
            + "&code=" + encode(code)
            + "&redirect_uri=" + encode(redirectUri)
            + "&client_id=" + encode(clientId)
            + "&code_verifier=" + encode(codeVerifier)
            + (resource != null ? "&resource=" + encode(resource) : "");
        return restClient.post()
            .uri(baseUrl() + "/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TokenResponse.class);
    }

    private TokenResponse refreshToken(String refreshToken, String clientId, String resource) {
        String form = "grant_type=refresh_token"
            + "&refresh_token=" + encode(refreshToken)
            + "&client_id=" + encode(clientId)
            + (resource != null ? "&resource=" + encode(resource) : "");
        return restClient.post()
            .uri(baseUrl() + "/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TokenResponse.class);
    }

    private McpSyncClient connectedMcpClient(String bearerToken) {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
            .builder(baseUrl())
            .endpoint("/mcp")
            .requestBuilder(java.net.http.HttpRequest.newBuilder().header("Authorization", "Bearer " + bearerToken))
            .build();
        McpSyncClient client = McpClient.sync(transport).build();
        client.initialize();
        openClients.add(client);
        return client;
    }

    // ------------------------------------------------------------------ tests

    @Test
    @DisplayName("fluxo completo: authorize -> consentimento -> code -> token -> chamada MCP real")
    void fullFlowIssuesWorkingAccessToken() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ_WRITE, pkce);

        TokenResponse tokens = exchangeCode(
            result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID, pkce.verifier(), properties.getResource());

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.tokenType()).isEqualTo("Bearer");
        assertThat(tokens.scope()).isEqualTo("read_write");

        McpSyncClient client = connectedMcpClient(tokens.accessToken());
        CallToolResult listResult = client.callTool(CallToolRequest.builder("list_my_lists").arguments(Map.of()).build());
        assertThat(listResult.isError()).isNotEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("client_id desconhecido em /oauth/authorize responde 400 direto (sem redirect)")
    void authorizeRejectsUnknownClient() {
        Pkce pkce = newPkce();
        URI uri = authorizeUri("does-not-exist", LOCAL_REDIRECT_URI, "read", "s", pkce.challenge(), "S256",
            properties.getResource());

        assertThatThrownBy(() -> callAuthorize(uri))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("redirect_uri não registrado responde 400 direto (evita open redirect)")
    void authorizeRejectsUnregisteredRedirectUri() {
        Pkce pkce = newPkce();
        URI uri = authorizeUri(LOCAL_CLIENT_ID, "http://evil.example.com/callback", "read", "s",
            pkce.challenge(), "S256", properties.getResource());

        assertThatThrownBy(() -> callAuthorize(uri))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("code_challenge_method=plain é rejeitado com redirect ?error= (OAuth 2.1 exige S256)")
    void authorizeRedirectsWithErrorForPlainPkce() {
        URI uri = authorizeUri(LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, "read", "s", "some-challenge", "plain",
            properties.getResource());

        ResponseEntity<Void> response = callAuthorize(uri);
        assertThat(response.getStatusCode().value()).isEqualTo(302);
        String location = response.getHeaders().getLocation().toString();
        assertThat(location).startsWith(LOCAL_REDIRECT_URI);
        assertThat(location).contains("error=invalid_request");
    }

    @Test
    @DisplayName("resource diferente do canônico é rejeitado com redirect ?error=invalid_target")
    void authorizeRedirectsWithErrorForWrongResource() {
        Pkce pkce = newPkce();
        URI uri = authorizeUri(LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, "read", "s", pkce.challenge(), "S256",
            "http://unknown-resource/mcp");

        ResponseEntity<Void> response = callAuthorize(uri);
        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation().toString()).contains("error=invalid_target");
    }

    @Test
    @DisplayName("cliente loopback (Claude Code) aceita redirect_uri com porta variável")
    void authorizeAllowsClaudeCodeLoopbackRedirectWithAnyPort() {
        Pkce pkce = newPkce();
        URI uri = authorizeUri(
            CLAUDE_CODE_CLIENT_ID, "http://localhost:54321/callback", "read", "s", pkce.challenge(), "S256",
            properties.getResource());

        ResponseEntity<Void> response = callAuthorize(uri);
        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation().toString()).contains("/oauth/consent?request_id=");
    }

    @Test
    @DisplayName("code_verifier incorreto é rejeitado no token endpoint (invalid_grant)")
    void tokenRejectsWrongCodeVerifier() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ, pkce);

        assertThatThrownBy(() -> exchangeCode(result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID,
            "wrong-verifier-wrong-verifier-wrong-verifier", properties.getResource()))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> {
                HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                assertThat(httpEx.getStatusCode().value()).isEqualTo(400);
                assertThat(httpEx.getResponseBodyAsString()).contains("invalid_grant");
            });
    }

    @Test
    @DisplayName("redirect_uri divergente do usado no authorize é rejeitado no token endpoint")
    void tokenRejectsRedirectUriMismatch() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ, pkce);

        assertThatThrownBy(() -> exchangeCode(
            result.get("code"), "http://localhost:8765/other-callback", LOCAL_CLIENT_ID, pkce.verifier(),
            properties.getResource()))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("resource divergente do usado no authorize é rejeitado no token endpoint (invalid_target)")
    void tokenRejectsResourceMismatch() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ, pkce);

        assertThatThrownBy(() -> exchangeCode(
            result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID, pkce.verifier(), "http://other-resource/mcp"))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> {
                HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                assertThat(httpEx.getStatusCode().value()).isEqualTo(400);
                assertThat(httpEx.getResponseBodyAsString()).contains("invalid_target");
            });
    }

    @Test
    @DisplayName("reenvio do mesmo authorization code (replay) revoga os tokens já emitidos por ele")
    void tokenReplayRevokesIssuedTokens() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ_WRITE, pkce);

        TokenResponse firstExchange = exchangeCode(
            result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID, pkce.verifier(), properties.getResource());

        // Replay do mesmo code
        assertThatThrownBy(() -> exchangeCode(
            result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID, pkce.verifier(), properties.getResource()))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(400));

        // O refresh token emitido pela troca original também deve ter sido revogado
        assertThatThrownBy(() -> refreshToken(firstExchange.refreshToken(), LOCAL_CLIENT_ID, properties.getResource()))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("refresh token rotaciona a cada uso e detecta reuso revogando a família inteira")
    void refreshRotatesAndDetectsReuse() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ_WRITE, pkce);
        TokenResponse initial = exchangeCode(
            result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID, pkce.verifier(), properties.getResource());

        TokenResponse rotated = refreshToken(initial.refreshToken(), LOCAL_CLIENT_ID, properties.getResource());
        assertThat(rotated.refreshToken()).isNotEqualTo(initial.refreshToken());
        assertThat(rotated.accessToken()).isNotBlank();

        // Reuso do refresh ANTIGO (já rotacionado) detecta vazamento e revoga a família:
        assertThatThrownBy(() -> refreshToken(initial.refreshToken(), LOCAL_CLIENT_ID, properties.getResource()))
            .isInstanceOf(HttpClientErrorException.class);

        // O refresh NOVO (rotated), da mesma família, também deve ter sido revogado.
        assertThatThrownBy(() -> refreshToken(rotated.refreshToken(), LOCAL_CLIENT_ID, properties.getResource()))
            .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    @DisplayName("POST /oauth/revoke invalida o refresh token informado")
    void revokeEndpointInvalidatesRefreshToken() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ_WRITE, pkce);
        TokenResponse tokens = exchangeCode(
            result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID, pkce.verifier(), properties.getResource());

        restClient.post()
            .uri(baseUrl() + "/oauth/revoke")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body("token=" + encode(tokens.refreshToken()) + "&client_id=" + encode(LOCAL_CLIENT_ID))
            .retrieve()
            .toBodilessEntity();

        assertThatThrownBy(() -> refreshToken(tokens.refreshToken(), LOCAL_CLIENT_ID, properties.getResource()))
            .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    @DisplayName("access token OAuth de escopo read bloqueia tool de mutação no /mcp")
    void readScopeOAuthTokenBlocksMutationTool() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ, pkce);
        TokenResponse tokens = exchangeCode(
            result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID, pkce.verifier(), properties.getResource());

        McpSyncClient client = connectedMcpClient(tokens.accessToken());
        CallToolResult createResult = client.callTool(
            CallToolRequest.builder("create_list").arguments(Map.of("name", "x", "type", "SHOPPING")).build());

        assertThat(createResult.isError()).isEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("access token OAuth do MCP não serve para a API REST do SPA (/api/**)")
    void oAuthAccessTokenDoesNotWorkOnApi() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ_WRITE, pkce);
        TokenResponse tokens = exchangeCode(
            result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID, pkce.verifier(), properties.getResource());

        // 401, não 403: o filtro do access token OAuth só atua em /mcp/**, então em
        // /api/** o token nem chega a ser reconhecido como credencial (equivalente a
        // anônimo) — Spring Security trata autorização negada de um anônimo como 401,
        // não 403. A defesa em profundidade em apiAccessManager() cobre o cenário em
        // que o filtro um dia deixe de restringir por path.
        assertThatThrownBy(() -> restClient.get()
            .uri(baseUrl() + "/api/lists")
            .header("Authorization", "Bearer " + tokens.accessToken())
            .retrieve()
            .toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(401));
    }

    @Test
    @DisplayName("access token com audience (resource) diferente do canônico é rejeitado no /mcp (401)")
    void oAuthAccessTokenWithWrongAudienceRejectedOnMcp() {
        User user = newUser();
        String wrongAudienceToken =
            oauthJwtService.issueAccessToken(user.getId(), LOCAL_CLIENT_ID, TokenScope.READ_WRITE, "http://other-resource/mcp");

        assertThatThrownBy(() -> restClient.post()
            .uri(baseUrl() + "/mcp")
            .header("Authorization", "Bearer " + wrongAudienceToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            .retrieve()
            .toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(401));
    }

    @Test
    @DisplayName("requisição sem token a /mcp inclui WWW-Authenticate apontando para o resource metadata")
    void unauthenticatedMcpRequestIncludesWwwAuthenticateHeader() {
        assertThatThrownBy(() -> restClient.post()
            .uri(baseUrl() + "/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            .retrieve()
            .toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> {
                HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                assertThat(httpEx.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                String wwwAuthenticate = httpEx.getResponseHeaders().getFirst("WWW-Authenticate");
                assertThat(wwwAuthenticate).contains("resource_metadata=");
                assertThat(wwwAuthenticate).contains("/.well-known/oauth-protected-resource");
            });
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("well-known endpoints expõem a metadata OAuth esperada (RFC 8414 / RFC 9728)")
    void wellKnownEndpointsExposeExpectedMetadata() {
        Map<String, Object> authServerMetadata = restClient.get()
            .uri(baseUrl() + "/.well-known/oauth-authorization-server")
            .retrieve()
            .body(Map.class);

        assertThat(authServerMetadata.get("issuer")).isEqualTo(properties.getIssuer());
        assertThat(authServerMetadata.get("authorization_endpoint")).isEqualTo(properties.getIssuer() + "/oauth/authorize");
        assertThat(authServerMetadata.get("token_endpoint")).isEqualTo(properties.getIssuer() + "/oauth/token");
        assertThat(authServerMetadata.get("revocation_endpoint")).isEqualTo(properties.getIssuer() + "/oauth/revoke");
        assertThat((List<String>) authServerMetadata.get("code_challenge_methods_supported")).containsExactly("S256");
        assertThat((List<String>) authServerMetadata.get("grant_types_supported"))
            .containsExactlyInAnyOrder("authorization_code", "refresh_token");
        assertThat((List<String>) authServerMetadata.get("scopes_supported"))
            .containsExactlyInAnyOrder("read", "read_write");

        Map<String, Object> resourceMetadata = restClient.get()
            .uri(baseUrl() + "/.well-known/oauth-protected-resource")
            .retrieve()
            .body(Map.class);

        assertThat(resourceMetadata.get("resource")).isEqualTo(properties.getResource());
        assertThat((List<String>) resourceMetadata.get("authorization_servers")).containsExactly(properties.getIssuer());
    }

    @Test
    @DisplayName("negar o consentimento devolve redirect com error=access_denied, sem emitir code")
    void denyConsentReturnsAccessDeniedRedirect() {
        User user = newUser();
        Pkce pkce = newPkce();
        String state = "deny-state-" + UUID.randomUUID();
        URI uri = authorizeUri(LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, "read", state, pkce.challenge(), "S256",
            properties.getResource());
        ResponseEntity<Void> authorizeResponse = callAuthorize(uri);
        String location = authorizeResponse.getHeaders().getLocation().toString();
        UUID requestId = UUID.fromString(location.substring(location.indexOf("request_id=") + "request_id=".length()));
        String consentCookie = extractConsentCookie(authorizeResponse);

        ConsentDecisionResponse decision = restClient.post()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId + "/deny")
            .header("Authorization", "Bearer " + sessionJwt(user))
            .header("Cookie", consentCookie)
            .retrieve()
            .body(ConsentDecisionResponse.class);

        assertThat(decision.redirectUrl()).startsWith(LOCAL_REDIRECT_URI);
        assertThat(decision.redirectUrl()).contains("error=access_denied");
        assertThat(decision.redirectUrl()).contains("state=" + state);
    }

    @Test
    @DisplayName("sequestro de consentimento cross-user (PoC do QA): aprovar sem o cookie do browser originador é bloqueado")
    void crossUserConsentHijackBlockedWithoutCookie() {
        // Ataque: alguém não-logado gera o pedido com o PRÓPRIO code_challenge
        // (nunca precisa logar para chamar /oauth/authorize) e manda só a URL de
        // consentimento (com o request_id) para a vítima via phishing — a vítima
        // jamais recebe o cookie que foi setado no browser de quem chamou authorize.
        Pkce attackerPkce = newPkce();
        String state = "hijack-" + UUID.randomUUID();
        URI uri = authorizeUri(LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, "read_write", state,
            attackerPkce.challenge(), "S256", properties.getResource());
        ResponseEntity<Void> authorizeResponse = callAuthorize(uri);
        String location = authorizeResponse.getHeaders().getLocation().toString();
        UUID requestId = UUID.fromString(location.substring(location.indexOf("request_id=") + "request_id=".length()));

        User victim = newUser();
        String victimJwt = sessionJwt(victim);

        // A vítima vê a tela de consentimento normalmente — GET não exige o cookie,
        // só reivindica o pedido para a conta dela.
        PendingAuthorizationView view = restClient.get()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId)
            .header("Authorization", "Bearer " + victimJwt)
            .retrieve()
            .body(PendingAuthorizationView.class);
        assertThat(view.clientId()).isEqualTo(LOCAL_CLIENT_ID);

        // ...mas aprovar SEM o cookie do browser originador é bloqueado (403) — sem
        // isso, o code sairia com o userId da vítima amarrado ao code_challenge de
        // quem gerou o pedido, permitindo resgatar tokens READ_WRITE da vítima.
        assertThatThrownBy(() -> restClient.post()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId + "/approve")
            .header("Authorization", "Bearer " + victimJwt)
            .retrieve()
            .toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(403));

        // Negar também exige o cookie.
        assertThatThrownBy(() -> restClient.post()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId + "/deny")
            .header("Authorization", "Bearer " + victimJwt)
            .retrieve()
            .toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @DisplayName("trava por usuário: ver/aprovar um pedido já reivindicado por OUTRA conta é bloqueado (403)")
    void approveByDifferentUserThanClaimedIsForbidden() {
        Pkce pkce = newPkce();
        String state = "ownership-" + UUID.randomUUID();
        URI uri = authorizeUri(LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, "read_write", state, pkce.challenge(), "S256",
            properties.getResource());
        ResponseEntity<Void> authorizeResponse = callAuthorize(uri);
        String location = authorizeResponse.getHeaders().getLocation().toString();
        UUID requestId = UUID.fromString(location.substring(location.indexOf("request_id=") + "request_id=".length()));
        String consentCookie = extractConsentCookie(authorizeResponse);

        User firstUser = newUser();
        User secondUser = newUser();

        // firstUser reivindica o pedido no primeiro GET autenticado.
        restClient.get()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId)
            .header("Authorization", "Bearer " + sessionJwt(firstUser))
            .retrieve()
            .body(PendingAuthorizationView.class);

        // secondUser nem consegue mais VER o pedido — já reivindicado por outra conta.
        assertThatThrownBy(() -> restClient.get()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId)
            .header("Authorization", "Bearer " + sessionJwt(secondUser))
            .retrieve()
            .toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(403));

        // ...e mesmo tendo o cookie correto, secondUser não consegue aprovar.
        assertThatThrownBy(() -> restClient.post()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId + "/approve")
            .header("Authorization", "Bearer " + sessionJwt(secondUser))
            .header("Cookie", consentCookie)
            .retrieve()
            .toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @DisplayName("LIMITAÇÃO CONHECIDA (D-021): access token OAuth continua válido até expirar mesmo após a família ser revogada")
    void revokedFamilyAccessTokenRemainsValidUntilNaturalExpiry() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ_WRITE, pkce);
        TokenResponse tokens = exchangeCode(
            result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID, pkce.verifier(), properties.getResource());

        restClient.post()
            .uri(baseUrl() + "/oauth/revoke")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body("token=" + encode(tokens.refreshToken()) + "&client_id=" + encode(LOCAL_CLIENT_ID))
            .retrieve()
            .toBodilessEntity();

        // O refresh já não funciona mais...
        assertThatThrownBy(() -> refreshToken(tokens.refreshToken(), LOCAL_CLIENT_ID, properties.getResource()))
            .isInstanceOf(HttpClientErrorException.class);

        // ...mas o ACCESS TOKEN já emitido, por ser um JWT stateless, PERMANECE
        // válido até expirar naturalmente — limitação conhecida e documentada em
        // docs/DECISIONS.md (D-021), mitigada com um TTL curto (10min). Este teste
        // documenta o comportamento esperado para não ser lido como bug no futuro.
        McpSyncClient client = connectedMcpClient(tokens.accessToken());
        CallToolResult listResult = client.callTool(CallToolRequest.builder("list_my_lists").arguments(Map.of()).build());
        assertThat(listResult.isError()).isNotEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("tela de Conexões lista e desconecta um assistente conectado via OAuth")
    void connectionsListAndRevoke() {
        User user = newUser();
        Pkce pkce = newPkce();
        Map<String, String> result = fullyAuthorize(
            sessionJwt(user), LOCAL_CLIENT_ID, LOCAL_REDIRECT_URI, TokenScope.READ_WRITE, pkce);
        TokenResponse tokens = exchangeCode(
            result.get("code"), LOCAL_REDIRECT_URI, LOCAL_CLIENT_ID, pkce.verifier(), properties.getResource());

        String sessionJwt = sessionJwt(user);
        List<OAuthConnectionSummary> connections = restClient.get()
            .uri(baseUrl() + "/api/oauth/connections")
            .header("Authorization", "Bearer " + sessionJwt)
            .retrieve()
            .body(List.class);
        assertThat(connections).isNotEmpty();

        restClient.post()
            .uri(baseUrl() + "/api/oauth/connections/" + LOCAL_CLIENT_ID + "/revoke")
            .header("Authorization", "Bearer " + sessionJwt)
            .retrieve()
            .toBodilessEntity();

        assertThatThrownBy(() -> refreshToken(tokens.refreshToken(), LOCAL_CLIENT_ID, properties.getResource()))
            .isInstanceOf(HttpClientErrorException.class);
    }
}
