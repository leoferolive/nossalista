package br.com.leoferolive.nossalista.mcpoauth.security;

import br.com.leoferolive.nossalista.config.Http401UnauthorizedEntryPoint;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Authentication entry point específico de {@code /mcp/**}, registrado via
 * {@code exceptionHandling().defaultAuthenticationEntryPointFor(...)} em
 * {@code SecurityConfig} — NÃO substitui o {@link Http401UnauthorizedEntryPoint}
 * global (usado por todo o resto da API), apenas adiciona o header
 * {@code WWW-Authenticate} exigido pela descoberta OAuth do MCP antes de
 * delegar o corpo/status 401 padrão RFC 7807 ao entry point global.
 *
 * <p>Implementado como wrapper em vez de editar {@link Http401UnauthorizedEntryPoint}
 * para não conflitar com trabalho em paralelo nesse arquivo (Fase D).</p>
 */
@Component
public class McpWwwAuthenticateEntryPoint implements AuthenticationEntryPoint {

    private final Http401UnauthorizedEntryPoint delegate;
    private final McpOAuthProperties properties;

    public McpWwwAuthenticateEntryPoint(Http401UnauthorizedEntryPoint delegate, McpOAuthProperties properties) {
        this.delegate = delegate;
        this.properties = properties;
    }

    @Override
    public void commence(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException authException
    ) throws IOException {
        String resourceMetadataUrl = properties.getIssuer() + "/.well-known/oauth-protected-resource";
        response.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + resourceMetadataUrl + "\"");
        delegate.commence(request, response, authException);
    }
}
