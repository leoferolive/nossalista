package br.com.leoferolive.nossalista.mcpoauth.controller;

import br.com.leoferolive.nossalista.mcpoauth.dto.OAuthConnectionSummary;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthConnectionService;
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

import java.util.List;

/**
 * Tela "Conexões": lista os assistentes conectados via OAuth (claude.ai,
 * Claude Code) e permite desconectá-los. Restrito a sessão JWT normal — mesma
 * regra de {@code /api/users/me/tokens/**} (um PAT não pode gerenciar outras
 * credenciais em nome do usuário).
 */
@RestController
@RequestMapping("/api/oauth/connections")
@Tag(name = "OAuth (MCP)", description = "Servidor de autorização OAuth 2.1 para clientes do servidor MCP")
public class McpOAuthConnectionsController {

    private final McpOAuthConnectionService connectionService;

    public McpOAuthConnectionsController(McpOAuthConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @GetMapping
    @Operation(summary = "Listar assistentes conectados via OAuth")
    public ResponseEntity<List<OAuthConnectionSummary>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(connectionService.list(user.getId()));
    }

    @PostMapping("/{clientId}/revoke")
    @Operation(summary = "Desconectar um assistente — revoga toda a família de refresh tokens desse cliente")
    public ResponseEntity<Void> revoke(@PathVariable String clientId, @AuthenticationPrincipal User user) {
        connectionService.disconnect(user.getId(), clientId);
        return ResponseEntity.noContent().build();
    }
}
