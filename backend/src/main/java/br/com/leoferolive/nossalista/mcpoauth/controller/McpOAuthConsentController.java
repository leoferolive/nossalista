package br.com.leoferolive.nossalista.mcpoauth.controller;

import br.com.leoferolive.nossalista.mcpoauth.dto.ConsentDecisionResponse;
import br.com.leoferolive.nossalista.mcpoauth.dto.PendingAuthorizationView;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthAuthorizationService;
import br.com.leoferolive.nossalista.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;

/**
 * Endpoints autenticados (JWT de sessão) por trás da tela de consentimento OAuth
 * da SPA ({@code /oauth/consent} no frontend). Restritos a sessão normal — um
 * Personal Access Token NUNCA pode aprovar/negar um consentimento OAuth em nome
 * do usuário (ver {@code SecurityConfig.sessionOnlyManager()}), mesmo padrão já
 * usado para {@code /api/users/me/tokens/**}.
 *
 * <p>Defesa contra sequestro de consentimento cross-user (achado do QA): todo
 * endpoint aqui passa o usuário autenticado para
 * {@link McpOAuthAuthorizationService}, que reivindica/valida a posse do
 * pedido; {@code approve}/{@code deny} exigem ADICIONALMENTE o cookie
 * {@link McpOAuthAuthorizationService#CONSENT_COOKIE_NAME} setado em
 * {@code GET /oauth/authorize} — ver Javadoc de {@code PendingAuthorization}.</p>
 */
@RestController
@RequestMapping("/api/oauth/consent")
@Tag(name = "OAuth (MCP)", description = "Servidor de autorização OAuth 2.1 para clientes do servidor MCP")
public class McpOAuthConsentController {

    private final McpOAuthAuthorizationService authorizationService;

    public McpOAuthConsentController(McpOAuthAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Carregar dados de um pedido de autorização pendente para a tela de consentimento")
    public ResponseEntity<PendingAuthorizationView> get(
        @PathVariable UUID requestId, @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(authorizationService.view(requestId, user.getId()));
    }

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "Aprovar o consentimento — emite o authorization code e devolve a URL de retorno ao cliente")
    public ResponseEntity<ConsentDecisionResponse> approve(
        @PathVariable UUID requestId, @AuthenticationPrincipal User user, HttpServletRequest request,
        HttpServletResponse response
    ) {
        String redirectUrl = authorizationService.approve(requestId, user.getId(), consentCookie(request));
        clearConsentCookie(response);
        return ResponseEntity.ok(new ConsentDecisionResponse(redirectUrl));
    }

    @PostMapping("/{requestId}/deny")
    @Operation(summary = "Negar o consentimento — devolve a URL de retorno ao cliente com error=access_denied")
    public ResponseEntity<ConsentDecisionResponse> deny(
        @PathVariable UUID requestId, @AuthenticationPrincipal User user, HttpServletRequest request,
        HttpServletResponse response
    ) {
        String redirectUrl = authorizationService.deny(requestId, user.getId(), consentCookie(request));
        clearConsentCookie(response);
        return ResponseEntity.ok(new ConsentDecisionResponse(redirectUrl));
    }

    /**
     * O pedido já foi decidido (approve ou deny) e removido — o cookie de
     * vínculo (ver {@code McpOAuthAuthorizationService.CONSENT_COOKIE_NAME}) não
     * tem mais utilidade e é limpo do browser (Max-Age=0), em vez de deixá-lo
     * expirar sozinho no TTL do pedido pendente.
     */
    private void clearConsentCookie(HttpServletResponse response) {
        ResponseCookie cleared = ResponseCookie.from(McpOAuthAuthorizationService.CONSENT_COOKIE_NAME, "")
            .path("/")
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .maxAge(0)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
    }

    private String consentCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
            .filter(c -> McpOAuthAuthorizationService.CONSENT_COOKIE_NAME.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
