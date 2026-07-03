package br.com.leoferolive.nossalista.mcpoauth;

import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.config.RateLimiterService;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.dto.ClientRegistrationResponse;
import br.com.leoferolive.nossalista.mcpoauth.dto.ConsentDecisionResponse;
import br.com.leoferolive.nossalista.mcpoauth.dto.PendingAuthorizationView;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthRegisteredClientRepository;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthAuthorizationService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
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
 * Testes de integração de {@code POST /oauth/register} (Dynamic Client
 * Registration, RFC 7591 — Fase C.1, docs/DECISIONS.md D-024) e do fluxo
 * ponta-a-ponta usando um {@code client_id} registrado DINAMICAMENTE em vez de
 * um dos clientes estáticos ({@code claude-ai}/{@code claude-code}).
 *
 * <p>Mesmo padrão de isolamento de {@code McpOAuthFlowIntegrationTest}: H2
 * dedicado via {@code @TestPropertySource}, nome de banco PRÓPRIO
 * ({@code mcp-oauth-dcr-it}) para não colidir com o daquela suíte.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:mcp-oauth-dcr-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
class McpOAuthDynamicClientRegistrationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private McpOAuthProperties properties;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private McpOAuthRegisteredClientRepository registeredClientRepository;

    private final List<McpSyncClient> openClients = new ArrayList<>();
    private final HttpClient noRedirectHttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    private final RestClient restClient = RestClient.builder()
        .requestFactory(new JdkClientHttpRequestFactory(noRedirectHttpClient))
        .build();

    @BeforeEach
    void resetRateLimiter() {
        // Isola o rate limit de registro entre testes — RateLimiterService é um
        // singleton in-memory compartilhado por todos os métodos desta classe
        // (mesmo padrão de AuthControllerTest).
        rateLimiterService.reset();
    }

    @AfterEach
    void closeClients() {
        openClients.forEach(McpSyncClient::close);
        openClients.clear();
    }

    // ---------------------------------------------------------------- helpers

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private User newUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userService.createUser(
            "dcruser" + suffix, "dcruser" + suffix + "@example.com", "hashed", "DCR User " + suffix,
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

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private ClientRegistrationResponse register(Map<String, Object> body) {
        return restClient.post()
            .uri(baseUrl() + "/oauth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(ClientRegistrationResponse.class);
    }

    /** Envia o corpo cru (sem serializar via Jackson) — usado para JSON deliberadamente inválido. */
    private void registerRaw(String rawJsonBody) {
        restClient.post()
            .uri(baseUrl() + "/oauth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(rawJsonBody)
            .retrieve()
            .toBodilessEntity();
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

    private String extractConsentCookie(ResponseEntity<Void> authorizeResponse) {
        List<String> setCookies = authorizeResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
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

    private String extractParam(String url, String name) {
        String marker = name + "=";
        int start = url.indexOf(marker);
        assertThat(start).as("param %s presente em %s", name, url).isGreaterThanOrEqualTo(0);
        int begin = start + marker.length();
        int end = url.indexOf('&', begin);
        String raw = end == -1 ? url.substring(begin) : url.substring(begin, end);
        return java.net.URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    private Map<String, String> exchangeCode(String code, String redirectUri, String clientId, String codeVerifier, String resource) {
        String form = "grant_type=authorization_code"
            + "&code=" + encode(code)
            + "&redirect_uri=" + encode(redirectUri)
            + "&client_id=" + encode(clientId)
            + "&code_verifier=" + encode(codeVerifier)
            + (resource != null ? "&resource=" + encode(resource) : "");
        @SuppressWarnings("unchecked")
        Map<String, String> response = restClient.post()
            .uri(baseUrl() + "/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);
        return response;
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
    @DisplayName("POST /oauth/register feliz: 201, client_id gerado, persistido, sem client_secret")
    void registerHappyPathReturnsCreatedClientPersisted() {
        ClientRegistrationResponse response = register(Map.of(
            "redirect_uris", List.of("https://app.example.com/callback"),
            "client_name", "Integration Test Client"
        ));

        assertThat(response.clientId()).startsWith("dcr_");
        assertThat(response.redirectUris()).containsExactly("https://app.example.com/callback");
        assertThat(response.tokenEndpointAuthMethod()).isEqualTo("none");
        assertThat(response.grantTypes()).containsExactly("authorization_code", "refresh_token");
        assertThat(response.responseTypes()).containsExactly("code");

        assertThat(registeredClientRepository.findByClientId(response.clientId())).isPresent();
    }

    @Test
    @DisplayName("redirect_uri http remoto (não-loopback) é rejeitado com 400 invalid_redirect_uri")
    void registerRejectsRemoteHttpRedirectUri() {
        assertThatThrownBy(() -> register(Map.of("redirect_uris", List.of("http://evil.example.com/callback"))))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> {
                HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                assertThat(httpEx.getStatusCode().value()).isEqualTo(400);
                assertThat(httpEx.getResponseBodyAsString()).contains("invalid_redirect_uri");
            });
    }

    @Test
    @DisplayName("redirect_uri loopback com porta explícita é aceito")
    void registerAcceptsLoopbackRedirectUriWithPort() {
        ClientRegistrationResponse response = register(Map.of(
            "redirect_uris", List.of("http://127.0.0.1:54321/callback")
        ));

        assertThat(response.clientId()).startsWith("dcr_");
    }

    @Test
    @DisplayName("sem redirect_uris responde 400 invalid_redirect_uri")
    void registerRejectsMissingRedirectUris() {
        assertThatThrownBy(() -> register(Map.of("client_name", "No redirect_uris")))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> {
                HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                assertThat(httpEx.getStatusCode().value()).isEqualTo(400);
                assertThat(httpEx.getResponseBodyAsString()).contains("invalid_redirect_uri");
            });
    }

    @Test
    @DisplayName("JSON sintaticamente inválido responde 400 invalid_client_metadata (achado do QA, não mais 500)")
    void registerRejectsMalformedJsonWithBadRequest() {
        assertThatThrownBy(() -> registerRaw("{ this is not valid json"))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> {
                HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                assertThat(httpEx.getStatusCode().value()).isEqualTo(400);
                assertThat(httpEx.getResponseBodyAsString()).contains("invalid_client_metadata");
            });
    }

    @Test
    @DisplayName("redirect_uris com tipo errado (string em vez de array) responde 400 invalid_client_metadata (achado do QA, não mais 500)")
    void registerRejectsWrongTypedField() {
        assertThatThrownBy(() -> registerRaw("{\"redirect_uris\": \"https://app.example.com/callback\"}"))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> {
                HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                assertThat(httpEx.getStatusCode().value()).isEqualTo(400);
                assertThat(httpEx.getResponseBodyAsString()).contains("invalid_client_metadata");
            });
    }

    @Test
    @DisplayName("client_name maior que a coluna (VARCHAR(200)) responde 400 invalid_client_metadata (achado do QA, não mais 500)")
    void registerRejectsOversizedClientName() {
        String oversizedName = "x".repeat(201);
        assertThatThrownBy(() -> register(Map.of(
            "redirect_uris", List.of("https://app.example.com/callback"),
            "client_name", oversizedName
        )))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> {
                HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                assertThat(httpEx.getStatusCode().value()).isEqualTo(400);
                assertThat(httpEx.getResponseBodyAsString()).contains("invalid_client_metadata");
            });
    }

    @Test
    @DisplayName("scope maior que a coluna (VARCHAR(50)) responde 400 invalid_client_metadata (achado do QA, não mais 500)")
    void registerRejectsOversizedScope() {
        String oversizedScope = "read ".repeat(15);
        assertThatThrownBy(() -> register(Map.of(
            "redirect_uris", List.of("https://app.example.com/callback"),
            "scope", oversizedScope
        )))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> {
                HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                assertThat(httpEx.getStatusCode().value()).isEqualTo(400);
                assertThat(httpEx.getResponseBodyAsString()).contains("invalid_client_metadata");
            });
    }

    @Test
    @DisplayName("redirect_uri loopback IPv6 ([::1]) com porta explícita é aceito (RFC 8252 §7.3, achado NIT do QA)")
    void registerAcceptsIpv6LoopbackRedirectUriWithPort() {
        ClientRegistrationResponse response = register(Map.of(
            "redirect_uris", List.of("http://[::1]:54321/callback")
        ));

        assertThat(response.clientId()).startsWith("dcr_");
        assertThat(response.redirectUris()).containsExactly("http://[::1]:54321/callback");
    }

    @Test
    @DisplayName("rate limit por IP em POST /oauth/register responde 429 too_many_requests")
    void registerEnforcesRateLimitPerIp() {
        int limit = properties.getDcr().getRateLimitPerIpPerHour();
        for (int i = 0; i < limit; i++) {
            register(Map.of("redirect_uris", List.of("https://app.example.com/callback-" + i)));
        }

        assertThatThrownBy(() -> register(Map.of("redirect_uris", List.of("https://app.example.com/callback-over"))))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> {
                HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                assertThat(httpEx.getStatusCode().value()).isEqualTo(429);
                assertThat(httpEx.getResponseBodyAsString()).contains("too_many_requests");
            });
    }

    @Test
    @DisplayName("discovery (RFC 8414) anuncia registration_endpoint quando DCR está habilitado")
    void discoveryAdvertisesRegistrationEndpoint() {
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = restClient.get()
            .uri(baseUrl() + "/.well-known/oauth-authorization-server")
            .retrieve()
            .body(Map.class);

        assertThat(metadata.get("registration_endpoint")).isEqualTo(properties.getIssuer() + "/oauth/register");
    }

    @Test
    @DisplayName("fluxo completo E2E: registrar via DCR -> authorize -> consentimento -> code -> token -> chamada MCP real")
    void fullDcrFlowIssuesWorkingAccessTokenForDynamicClient() {
        String redirectUri = "http://127.0.0.1:58421/callback";
        ClientRegistrationResponse registration = register(Map.of("redirect_uris", List.of(redirectUri)));
        String clientId = registration.clientId();

        User user = newUser();
        Pkce pkce = newPkce();
        String state = "dcr-state-" + UUID.randomUUID();
        URI uri = authorizeUri(clientId, redirectUri, "read_write", state, pkce.challenge(), "S256", properties.getResource());

        ResponseEntity<Void> authorizeResponse = callAuthorize(uri);
        assertThat(authorizeResponse.getStatusCode().value()).isEqualTo(302);
        String consentCookie = extractConsentCookie(authorizeResponse);
        String location = authorizeResponse.getHeaders().getLocation().toString();
        UUID requestId = UUID.fromString(location.substring(location.indexOf("request_id=") + "request_id=".length()));

        // Achado do review (I-1): GET /oauth/authorize sozinho NUNCA marca "usado" —
        // só a troca code->token, mais abaixo, faz isso.
        assertThat(registeredClientRepository.findByClientId(clientId).orElseThrow().getLastUsedAt()).isNull();

        String sessionJwt = sessionJwt(user);
        PendingAuthorizationView view = restClient.get()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId)
            .header("Authorization", "Bearer " + sessionJwt)
            .header("Cookie", consentCookie)
            .retrieve()
            .body(PendingAuthorizationView.class);
        assertThat(view.clientId()).isEqualTo(clientId);

        ConsentDecisionResponse decision = restClient.post()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId + "/approve")
            .header("Authorization", "Bearer " + sessionJwt)
            .header("Cookie", consentCookie)
            .retrieve()
            .body(ConsentDecisionResponse.class);
        String code = extractParam(decision.redirectUrl(), "code");

        Map<String, String> tokens = exchangeCode(code, redirectUri, clientId, pkce.verifier(), properties.getResource());
        assertThat(tokens.get("access_token")).isNotBlank();
        assertThat(tokens.get("refresh_token")).isNotBlank();

        // O client dinâmico deve ter sido marcado como "usado" ao completar o fluxo.
        assertThat(registeredClientRepository.findByClientId(clientId).orElseThrow().getLastUsedAt()).isNotNull();

        McpSyncClient mcpClient = connectedMcpClient(tokens.get("access_token"));
        CallToolResult listResult = mcpClient.callTool(CallToolRequest.builder("list_my_lists").arguments(Map.of()).build());
        assertThat(listResult.isError()).isNotEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("achado do review (I-1): GET /oauth/authorize repetido, sem nunca completar o fluxo, NÃO imuniza o client contra o cleanup")
    void repeatedAuthorizeWithoutCompletingFlowNeverMarksClientAsUsed() {
        // PoC do ataque: um client dinâmico registrado (o próprio atacante), sem
        // NENHUMA credencial de sessão, chama /oauth/authorize repetidamente —
        // isso sozinho não deveria conseguir "imunizar" o client contra
        // McpOAuthCleanupScheduler (que só remove last_used_at IS NULL).
        String redirectUri = "http://127.0.0.1:58424/callback";
        ClientRegistrationResponse registration = register(Map.of("redirect_uris", List.of(redirectUri)));
        String clientId = registration.clientId();

        for (int i = 0; i < 5; i++) {
            Pkce pkce = newPkce();
            URI uri = authorizeUri(clientId, redirectUri, "read", "state-" + i, pkce.challenge(), "S256",
                properties.getResource());
            ResponseEntity<Void> response = callAuthorize(uri);
            assertThat(response.getStatusCode().value()).isEqualTo(302);
        }

        assertThat(registeredClientRepository.findByClientId(clientId).orElseThrow().getLastUsedAt())
            .as("last_used_at deve continuar NULL — o client permanece elegível para a varredura de limpeza")
            .isNull();
    }

    @Test
    @DisplayName("cliente dinâmico continua protegido pela trava de sequestro de consentimento cross-user (D-022)")
    void dynamicClientCrossUserConsentHijackIsBlockedWithoutCookie() {
        String redirectUri = "http://127.0.0.1:58422/callback";
        ClientRegistrationResponse registration = register(Map.of("redirect_uris", List.of(redirectUri)));
        String clientId = registration.clientId();

        Pkce attackerPkce = newPkce();
        String state = "dcr-hijack-" + UUID.randomUUID();
        URI uri = authorizeUri(clientId, redirectUri, "read_write", state, attackerPkce.challenge(), "S256",
            properties.getResource());
        ResponseEntity<Void> authorizeResponse = callAuthorize(uri);
        String location = authorizeResponse.getHeaders().getLocation().toString();
        UUID requestId = UUID.fromString(location.substring(location.indexOf("request_id=") + "request_id=".length()));

        User victim = newUser();
        String victimJwt = sessionJwt(victim);

        PendingAuthorizationView view = restClient.get()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId)
            .header("Authorization", "Bearer " + victimJwt)
            .retrieve()
            .body(PendingAuthorizationView.class);
        assertThat(view.clientId()).isEqualTo(clientId);

        assertThatThrownBy(() -> restClient.post()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId + "/approve")
            .header("Authorization", "Bearer " + victimJwt)
            .retrieve()
            .toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @DisplayName("client_id de um cliente DINÂMICO com escopo READ bloqueia tool de mutação no /mcp (mesma regra dos clientes estáticos)")
    void dynamicClientReadScopeTokenBlocksMutationTool() {
        String redirectUri = "http://127.0.0.1:58423/callback";
        ClientRegistrationResponse registration = register(Map.of("redirect_uris", List.of(redirectUri)));
        String clientId = registration.clientId();

        User user = newUser();
        Pkce pkce = newPkce();
        String state = "dcr-read-" + UUID.randomUUID();
        URI uri = authorizeUri(clientId, redirectUri, "read", state, pkce.challenge(), "S256", properties.getResource());
        ResponseEntity<Void> authorizeResponse = callAuthorize(uri);
        String location = authorizeResponse.getHeaders().getLocation().toString();
        UUID requestId = UUID.fromString(location.substring(location.indexOf("request_id=") + "request_id=".length()));
        String consentCookie = extractConsentCookie(authorizeResponse);

        String sessionJwt = sessionJwt(user);
        ConsentDecisionResponse decision = restClient.post()
            .uri(baseUrl() + "/api/oauth/consent/" + requestId + "/approve")
            .header("Authorization", "Bearer " + sessionJwt)
            .header("Cookie", consentCookie)
            .retrieve()
            .body(ConsentDecisionResponse.class);
        String code = extractParam(decision.redirectUrl(), "code");

        Map<String, String> tokens = exchangeCode(code, redirectUri, clientId, pkce.verifier(), properties.getResource());
        assertThat(tokens.get("scope")).isEqualTo("read");

        McpSyncClient mcpClient = connectedMcpClient(tokens.get("access_token"));
        CallToolResult createResult = mcpClient.callTool(
            CallToolRequest.builder("create_list").arguments(Map.of("name", "x", "type", "SHOPPING")).build());
        assertThat(createResult.isError()).isEqualTo(Boolean.TRUE);
    }
}
