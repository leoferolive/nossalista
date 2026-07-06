package br.com.leoferolive.nossalista.mcpoauth.security;

import br.com.leoferolive.nossalista.config.ClientIpResolver;
import br.com.leoferolive.nossalista.config.Http401UnauthorizedEntryPoint;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * <p>Loga cada negação 401 em {@code /mcp} com o IP real do cliente
 * ({@code CF-Connecting-IP}, resolvido por {@link ClientIpResolver}) e o
 * {@code User-Agent}. Como o servidor MCP e OAuth expostos convidam clientes
 * automatizados (e crawlers) da internet, este e o unico ponto que registra
 * QUEM tenta acessar o {@code /mcp} sem credencial valida — sem ele, uma
 * negacao 401 e silenciosa e so um {@code tcpdump} identificaria o cliente.
 * Nivel WARN para ser greppavel; o volume acompanha as tentativas negadas
 * (baixo em operacao normal).</p>
 */
@Component
public class McpWwwAuthenticateEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(McpWwwAuthenticateEntryPoint.class);

    private final Http401UnauthorizedEntryPoint delegate;
    private final McpOAuthProperties properties;
    private final ClientIpResolver clientIpResolver;

    public McpWwwAuthenticateEntryPoint(
        Http401UnauthorizedEntryPoint delegate,
        McpOAuthProperties properties,
        ClientIpResolver clientIpResolver
    ) {
        this.delegate = delegate;
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    public void commence(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException authException
    ) throws IOException {
        String userAgent = request.getHeader("User-Agent");
        log.warn("MCP auth denied (401): method={} uri={} ip={} userAgent=\"{}\" reason={}",
            request.getMethod(),
            request.getRequestURI(),
            clientIpResolver.resolve(request),
            userAgent != null ? userAgent : "",
            authException.getMessage());

        String resourceMetadataUrl = properties.getIssuer() + "/.well-known/oauth-protected-resource";
        response.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + resourceMetadataUrl + "\"");
        delegate.commence(request, response, authException);
    }
}
