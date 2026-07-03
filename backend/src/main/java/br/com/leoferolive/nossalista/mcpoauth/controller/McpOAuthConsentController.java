package br.com.leoferolive.nossalista.mcpoauth.controller;

import br.com.leoferolive.nossalista.mcpoauth.dto.ConsentDecisionResponse;
import br.com.leoferolive.nossalista.mcpoauth.dto.PendingAuthorizationView;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthAuthorizationService;
import br.com.leoferolive.nossalista.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints autenticados (JWT de sessão) por trás da tela de consentimento OAuth
 * da SPA ({@code /oauth/consent} no frontend). Restritos a sessão normal — um
 * Personal Access Token NUNCA pode aprovar/negar um consentimento OAuth em nome
 * do usuário (ver {@code SecurityConfig.sessionOnlyManager()}), mesmo padrão já
 * usado para {@code /api/users/me/tokens/**}.
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
    public ResponseEntity<PendingAuthorizationView> get(@PathVariable UUID requestId) {
        return ResponseEntity.ok(authorizationService.view(requestId));
    }

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "Aprovar o consentimento — emite o authorization code e devolve a URL de retorno ao cliente")
    public ResponseEntity<ConsentDecisionResponse> approve(
        @PathVariable UUID requestId, @AuthenticationPrincipal User user
    ) {
        String redirectUrl = authorizationService.approve(requestId, user.getId());
        return ResponseEntity.ok(new ConsentDecisionResponse(redirectUrl));
    }

    @PostMapping("/{requestId}/deny")
    @Operation(summary = "Negar o consentimento — devolve a URL de retorno ao cliente com error=access_denied")
    public ResponseEntity<ConsentDecisionResponse> deny(@PathVariable UUID requestId) {
        String redirectUrl = authorizationService.deny(requestId);
        return ResponseEntity.ok(new ConsentDecisionResponse(redirectUrl));
    }
}
