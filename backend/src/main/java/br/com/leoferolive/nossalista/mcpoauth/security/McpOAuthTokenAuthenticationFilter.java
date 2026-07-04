package br.com.leoferolive.nossalista.mcpoauth.security;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.config.AuthenticatedUserCache;
import br.com.leoferolive.nossalista.config.PatAuthorizationSupport;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthJwtService;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthJwtService.McpOAuthAccessTokenClaims;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro de autenticação via access token OAuth do servidor MCP (Fase C — ver
 * docs/DECISIONS.md D-022).
 *
 * <p>Só atua em requisições a {@code /mcp/**} — os escopos OAuth do MCP valem
 * EXCLUSIVAMENTE para o servidor MCP, nunca para {@code /api/**} (reforçado em
 * profundidade por {@code SecurityConfig.apiAccessManager()}, que nega
 * explicitamente qualquer autenticação marcada com
 * {@link PatAuthorizationSupport#MCP_OAUTH_AUTHORITY}). Fora de {@code /mcp/**}
 * a requisição segue intocada para o {@link br.com.leoferolive.nossalista.config.JwtAuthenticationFilter}.</p>
 *
 * <p>Tenta validar o Bearer token como JWT assinado com
 * {@code MCP_OAUTH_SIGNING_KEY}; se a assinatura não bater (é um JWT de sessão
 * normal, assinado com {@code JWT_SECRET}) ou o token não for um JWT válido,
 * simplesmente segue a cadeia sem autenticar — o
 * {@code JwtAuthenticationFilter} seguinte tenta validá-lo como sessão normal.</p>
 */
@Component
public class McpOAuthTokenAuthenticationFilter extends OncePerRequestFilter {

    private final McpOAuthJwtService jwtService;
    private final AuthenticatedUserCache userCache;

    public McpOAuthTokenAuthenticationFilter(McpOAuthJwtService jwtService, AuthenticatedUserCache userCache) {
        this.jwtService = jwtService;
        this.userCache = userCache;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/mcp")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        Optional<McpOAuthAccessTokenClaims> claims = jwtService.validate(token);
        if (claims.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        McpOAuthAccessTokenClaims accessTokenClaims = claims.get();
        User user = userCache.findById(accessTokenClaims.userId()).orElse(null);
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            user, null, authoritiesFor(user, accessTokenClaims.scope()));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /**
     * Re-executa a autenticação também no dispatch ASYNC (streaming MCP). Este é o
     * caminho de auth mais crítico para o {@code /mcp}: o próprio access token OAuth
     * do MCP. Ver a explicação completa em
     * {@code JwtAuthenticationFilter#shouldNotFilterAsyncDispatch()} e
     * docs/DECISIONS.md D-023 — sem isto, o {@code AuthorizationFilter} no segundo
     * passo do {@code AsyncContext.dispatch()} do transporte Streamable HTTP nega o
     * token válido por falta de {@code SecurityContext}, virando 500 sobre o SSE.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    private List<GrantedAuthority> authoritiesFor(User user, TokenScope scope) {
        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        return List.of(
            new SimpleGrantedAuthority("ROLE_" + role.name()),
            new SimpleGrantedAuthority(PatAuthorizationSupport.PAT_AUTHORITY),
            new SimpleGrantedAuthority(PatAuthorizationSupport.MCP_OAUTH_AUTHORITY),
            new SimpleGrantedAuthority(scope.authority())
        );
    }
}
