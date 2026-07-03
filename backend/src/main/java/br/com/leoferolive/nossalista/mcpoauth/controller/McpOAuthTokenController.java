package br.com.leoferolive.nossalista.mcpoauth.controller;

import br.com.leoferolive.nossalista.mcpoauth.dto.TokenResponse;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthTokenException;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /oauth/token} e {@code POST /oauth/revoke} — endpoints públicos
 * do servidor de autorização OAuth do MCP (RFC 6749 §3.2 / RFC 7009). Públicos
 * porque os clientes suportados (claude.ai, Claude Code) são clientes PÚBLICOS
 * (PKCE, sem {@code client_secret}) — a prova de posse é o {@code code_verifier}
 * (na troca do code) ou a posse do próprio refresh token opaco (na rotação).
 *
 * <p>Body {@code application/x-www-form-urlencoded}, igual ao endpoint de token
 * padrão OAuth2 — diferente do resto da API (JSON), por exigência dos SDKs
 * clientes que consomem este endpoint.</p>
 */
@RestController
@Tag(name = "OAuth (MCP)", description = "Servidor de autorização OAuth 2.1 para clientes do servidor MCP")
public class McpOAuthTokenController {

    private final McpOAuthTokenService tokenService;

    public McpOAuthTokenController(McpOAuthTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(
        summary = "Trocar authorization code (+ PKCE) ou refresh token por um novo access token",
        description = "grant_type=authorization_code (code, redirect_uri, client_id, code_verifier, resource) "
            + "ou grant_type=refresh_token (refresh_token, client_id, resource). Refresh tokens rotacionam a "
            + "cada uso; reuso de um refresh já rotacionado revoga a família inteira."
    )
    public ResponseEntity<TokenResponse> token(
        @RequestParam("grant_type") String grantType,
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "redirect_uri", required = false) String redirectUri,
        @RequestParam(value = "client_id", required = false) String clientId,
        @RequestParam(value = "code_verifier", required = false) String codeVerifier,
        @RequestParam(value = "refresh_token", required = false) String refreshToken,
        @RequestParam(value = "resource", required = false) String resource
    ) {
        if (clientId == null || clientId.isBlank()) {
            throw OAuthTokenException.invalidRequest("client_id is required.");
        }

        TokenResponse response = switch (grantType) {
            case "authorization_code" -> {
                requireNonBlank(code, "code");
                requireNonBlank(redirectUri, "redirect_uri");
                requireNonBlank(codeVerifier, "code_verifier");
                yield tokenService.exchangeAuthorizationCode(code, redirectUri, clientId, codeVerifier, resource);
            }
            case "refresh_token" -> {
                requireNonBlank(refreshToken, "refresh_token");
                yield tokenService.refresh(refreshToken, clientId, resource);
            }
            default -> throw OAuthTokenException.unsupportedGrantType(
                "Supported grant types: authorization_code, refresh_token.");
        };

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/oauth/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(
        summary = "Revogar um refresh (ou access) token",
        description = "RFC 7009: sempre responde 200, mesmo para token desconhecido/já revogado "
            + "(nunca revela se um token era válido). Revoga a família inteira do token informado."
    )
    public ResponseEntity<Void> revoke(
        @RequestParam("token") String token,
        @RequestParam(value = "client_id", required = false) String clientId
    ) {
        tokenService.revoke(token, clientId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private void requireNonBlank(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw OAuthTokenException.invalidRequest(paramName + " is required.");
        }
    }
}
