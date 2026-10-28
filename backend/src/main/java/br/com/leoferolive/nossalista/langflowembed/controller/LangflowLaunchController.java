package br.com.leoferolive.nossalista.langflowembed.controller;

import br.com.leoferolive.nossalista.langflowembed.config.LangflowEmbedProperties;
import br.com.leoferolive.nossalista.langflowembed.service.LangflowEmbedCookieService;
import br.com.leoferolive.nossalista.langflowembed.service.LangflowEmbedTokenService;
import br.com.leoferolive.nossalista.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Abre o Langflow para o usuário logado: grava o token do embed em cookie e redireciona.
 *
 * <p>A autorização exige sessão web (regra em {@code SecurityConfig}); PAT e tokens
 * OAuth do MCP não passam por aqui, para não emitirem sessão do Langflow.</p>
 */
@RestController
@Tag(name = "Langflow", description = "Embed do Langflow (spike)")
public class LangflowLaunchController {

    private final LangflowEmbedProperties properties;
    private final LangflowEmbedTokenService tokenService;
    private final LangflowEmbedCookieService cookieService;

    public LangflowLaunchController(LangflowEmbedProperties properties, LangflowEmbedTokenService tokenService,
                                    LangflowEmbedCookieService cookieService) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.cookieService = cookieService;
    }

    @GetMapping("/api/langflow/launch")
    @Operation(summary = "Grava o token do Langflow em cookie e redireciona para o Langflow")
    public ResponseEntity<Void> launch(@AuthenticationPrincipal User user) {
        if (!properties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, cookieService.launchCookie(tokenService.issue(user)).toString())
            .location(URI.create(properties.getLaunchRedirect()))
            .build();
    }
}
