package br.com.leoferolive.nossalista.mcpoauth.controller;

import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de descoberta OAuth públicos exigidos pela especificação de
 * autorização do MCP: RFC 8414 (metadata do servidor de autorização) e RFC
 * 9728 (metadata do resource protegido). Clientes como claude.ai consultam
 * esses endpoints antes de iniciar o fluxo de authorize.
 */
@RestController
@Tag(name = "OAuth (MCP)", description = "Servidor de autorização OAuth 2.1 para clientes do servidor MCP")
public class WellKnownOAuthController {

    private final McpOAuthProperties properties;

    public WellKnownOAuthController(McpOAuthProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    @Operation(summary = "Metadata do servidor de autorização OAuth (RFC 8414)")
    public ResponseEntity<Map<String, Object>> authorizationServerMetadata() {
        String issuer = properties.getIssuer();
        Map<String, Object> metadata = Map.ofEntries(
            Map.entry("issuer", issuer),
            Map.entry("authorization_endpoint", issuer + "/oauth/authorize"),
            Map.entry("token_endpoint", issuer + "/oauth/token"),
            Map.entry("revocation_endpoint", issuer + "/oauth/revoke"),
            Map.entry("response_types_supported", List.of("code")),
            Map.entry("grant_types_supported", List.of("authorization_code", "refresh_token")),
            Map.entry("code_challenge_methods_supported", List.of("S256")),
            Map.entry("token_endpoint_auth_methods_supported", List.of("none")),
            Map.entry("scopes_supported", List.of("read", "read_write"))
        );
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/.well-known/oauth-protected-resource")
    @Operation(summary = "Metadata do resource protegido /mcp (RFC 9728)")
    public ResponseEntity<Map<String, Object>> protectedResourceMetadata() {
        Map<String, Object> metadata = Map.of(
            "resource", properties.getResource(),
            "authorization_servers", List.of(properties.getIssuer()),
            "bearer_methods_supported", List.of("header"),
            "scopes_supported", List.of("read", "read_write")
        );
        return ResponseEntity.ok(metadata);
    }
}
