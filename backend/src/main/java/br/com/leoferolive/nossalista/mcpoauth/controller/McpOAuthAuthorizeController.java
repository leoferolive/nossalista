package br.com.leoferolive.nossalista.mcpoauth.controller;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties.ClientDefinition;
import br.com.leoferolive.nossalista.mcpoauth.domain.PendingAuthorization;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthAuthorizationService;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthClientRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * {@code GET /oauth/authorize} — início do fluxo Authorization Code + PKCE
 * (OAuth 2.1) do servidor MCP. Público (sem exigir sessão): o browser chega
 * aqui numa navegação top-level vindo do cliente OAuth (claude.ai, Claude
 * Code), e o JWT de sessão do usuário vive em {@code localStorage} — não é
 * enviado automaticamente numa navegação de página inteira.
 *
 * <p>Por isso o endpoint SEMPRE redireciona para a tela de consentimento da
 * SPA ({@code {FRONTEND_URL}/oauth/consent?request_id=...}), que decide se o
 * usuário precisa logar primeiro (via o fluxo de login já existente) antes de
 * chamar os endpoints autenticados de consentimento. Ver docs/DECISIONS.md D-022.</p>
 */
@Controller
@Tag(name = "OAuth (MCP)", description = "Servidor de autorização OAuth 2.1 para clientes do servidor MCP")
public class McpOAuthAuthorizeController {

    /**
     * Mesmo limite da coluna {@code mcp_oauth_pending_authorizations.state}
     * (VARCHAR(512), migration V13) — sem este guard, um {@code state}
     * excedendo o limite quebraria com uma exceção de truncamento do banco
     * (500 genérico) em vez de um erro OAuth tratado (achado do review, NIT).
     */
    private static final int MAX_STATE_LENGTH = 512;

    private final McpOAuthClientRegistry clientRegistry;
    private final McpOAuthAuthorizationService authorizationService;
    private final McpOAuthProperties properties;

    @Value("${frontend.url}")
    private String frontendUrl;

    public McpOAuthAuthorizeController(
        McpOAuthClientRegistry clientRegistry,
        McpOAuthAuthorizationService authorizationService,
        McpOAuthProperties properties
    ) {
        this.clientRegistry = clientRegistry;
        this.authorizationService = authorizationService;
        this.properties = properties;
    }

    @GetMapping("/oauth/authorize")
    @Operation(
        summary = "Iniciar autorização OAuth 2.1 (Authorization Code + PKCE)",
        description = "Valida o pedido e redireciona para a tela de consentimento da SPA. "
            + "client_id/redirect_uri inválidos respondem 400 direto (nunca redirecionam, evita open redirect); "
            + "demais erros de validação redirecionam com ?error= para o redirect_uri do cliente."
    )
    public void authorize(
        @RequestParam("response_type") String responseType,
        @RequestParam("client_id") String clientId,
        @RequestParam("redirect_uri") String redirectUri,
        @RequestParam(value = "scope", defaultValue = "read") String scopeParam,
        @RequestParam(value = "state", required = false) String state,
        @RequestParam("code_challenge") String codeChallenge,
        @RequestParam("code_challenge_method") String codeChallengeMethod,
        @RequestParam("resource") String resource,
        HttpServletResponse httpResponse
    ) throws IOException {
        // client_id/redirect_uri desconhecidos: NUNCA redirecionar (o destino não é
        // confiável) — 400 direto lançado pelas próprias validações abaixo.
        ClientDefinition client = clientRegistry.require(clientId);
        clientRegistry.validateRedirectUri(client, redirectUri);

        // A partir daqui redirect_uri é confiável: demais erros voltam como
        // ?error=...&state=... para o próprio cliente OAuth (RFC 6749 §4.1.2.1).
        if (state != null && state.length() > MAX_STATE_LENGTH) {
            // state não é ecoado aqui: um valor esse grande já não é o state
            // legítimo do cliente, e ecoá-lo de volta não teria utilidade.
            redirectWithError(httpResponse, redirectUri, null, "invalid_request",
                "state exceeds the maximum supported length (" + MAX_STATE_LENGTH + " characters).");
            return;
        }
        if (!"code".equals(responseType)) {
            redirectWithError(httpResponse, redirectUri, state, "unsupported_response_type",
                "Only response_type=code is supported.");
            return;
        }
        if (!"S256".equals(codeChallengeMethod)) {
            redirectWithError(httpResponse, redirectUri, state, "invalid_request",
                "code_challenge_method must be S256. plain is not supported (OAuth 2.1).");
            return;
        }
        if (codeChallenge == null || codeChallenge.isBlank()) {
            redirectWithError(httpResponse, redirectUri, state, "invalid_request", "code_challenge is required.");
            return;
        }
        if (!properties.getResource().equals(resource)) {
            redirectWithError(httpResponse, redirectUri, state, "invalid_target",
                "Unknown resource. Expected: " + properties.getResource());
            return;
        }

        TokenScope scope = parseScope(scopeParam);
        if (scope == null) {
            redirectWithError(httpResponse, redirectUri, state, "invalid_scope",
                "Supported scopes: read, read_write.");
            return;
        }

        PendingAuthorization pending =
            authorizationService.createPending(clientId, redirectUri, scope, state, codeChallenge, resource);

        // Vincula o pedido a ESTE browser (defesa contra sequestro de consentimento
        // cross-user, achado do QA — ver Javadoc de PendingAuthorization/D-022):
        // approve/deny exigem esse cookie batendo com o nonce persistido.
        ResponseCookie cookie = ResponseCookie.from(McpOAuthAuthorizationService.CONSENT_COOKIE_NAME, pending.getNonce())
            .path("/")
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .maxAge(properties.getPendingAuthorizationTtl())
            .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String consentUrl = frontendUrl + "/oauth/consent?request_id=" + pending.getId();
        httpResponse.sendRedirect(consentUrl);
    }

    private TokenScope parseScope(String scopeParam) {
        if (scopeParam == null) {
            return null;
        }
        String normalized = scopeParam.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "read" -> TokenScope.READ;
            case "read_write" -> TokenScope.READ_WRITE;
            default -> null;
        };
    }

    private void redirectWithError(
        HttpServletResponse response, String redirectUri, String state, String error, String description
    ) throws IOException {
        StringBuilder sb = new StringBuilder(redirectUri);
        sb.append(redirectUri.contains("?") ? '&' : '?');
        sb.append("error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
        sb.append("&error_description=").append(URLEncoder.encode(description, StandardCharsets.UTF_8));
        if (state != null) {
            sb.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        response.sendRedirect(sb.toString());
    }
}
