package br.com.leoferolive.nossalista.mcpoauth.controller;

import br.com.leoferolive.nossalista.config.ClientIpResolver;
import br.com.leoferolive.nossalista.config.RateLimiterService;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.dto.ClientRegistrationRequest;
import br.com.leoferolive.nossalista.mcpoauth.dto.ClientRegistrationResponse;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthClientRegistrationException;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthDynamicClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * {@code POST /oauth/register} — Dynamic Client Registration (RFC 7591) para
 * o servidor de autorização OAuth do MCP. PÚBLICO por natureza da RFC (o
 * cliente ainda não tem credencial nenhuma nesta etapa) mas ENDURECIDO: rate
 * limit por IP aqui no controller, hardening de {@code redirect_uris} e teto
 * global de clientes em {@link McpOAuthDynamicClientService}.
 *
 * <p>Fase C.1 do roadmap MCP (complementa a Fase C/D-022) — destrava o botão
 * "Add connector" do claude.ai, que tenta DCR automático e falha com
 * {@code oauth_error=registration_endpoint_missing} sem este endpoint. Ver
 * docs/DECISIONS.md D-024.</p>
 */
@RestController
@Tag(name = "OAuth (MCP)", description = "Servidor de autorização OAuth 2.1 para clientes do servidor MCP")
public class McpOAuthClientRegistrationController {

    private final McpOAuthDynamicClientService clientService;
    private final McpOAuthProperties properties;
    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;

    public McpOAuthClientRegistrationController(
        McpOAuthDynamicClientService clientService,
        McpOAuthProperties properties,
        RateLimiterService rateLimiterService,
        ClientIpResolver clientIpResolver
    ) {
        this.clientService = clientService;
        this.properties = properties;
        this.rateLimiterService = rateLimiterService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping(
        value = "/oauth/register",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Dynamic Client Registration (RFC 7591)",
        description = "Registra um cliente OAuth público (PKCE-only, sem client_secret) dinamicamente. "
            + "redirect_uris deve ser https ou loopback com porta explícita; rate limit por IP; "
            + "erros no formato OAuth padrão ({\"error\", \"error_description\"})."
    )
    public ResponseEntity<ClientRegistrationResponse> register(
        @RequestBody(required = false) ClientRegistrationRequest request, HttpServletRequest httpRequest
    ) {
        if (!properties.getDcr().isEnabled()) {
            throw OAuthClientRegistrationException.disabled("Dynamic Client Registration is disabled on this server.");
        }

        String clientIp = clientIpResolver.resolve(httpRequest);
        boolean allowed = rateLimiterService.isAllowed(
            "mcp-oauth-register:ip:" + clientIp, properties.getDcr().getRateLimitPerIpPerHour(), Duration.ofHours(1));
        if (!allowed) {
            throw OAuthClientRegistrationException.tooManyRequests(
                "Too many client registration requests from this IP. Try again later.");
        }

        ClientRegistrationResponse response = clientService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
