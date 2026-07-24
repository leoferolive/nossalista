package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.auth.service.SessionCookieService;
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
import java.util.UUID;

/**
 * Filtro da sessão web que valida o JWT armazenado exclusivamente no cookie
 * HttpOnly. JWTs de sessão enviados em {@code Authorization} são ignorados;
 * esse header permanece reservado para PATs e OAuth do MCP.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthenticatedUserCache userCache;
    private final SessionCookieService sessionCookieService;

    public JwtAuthenticationFilter(JwtService jwtService, AuthenticatedUserCache userCache,
                                   SessionCookieService sessionCookieService) {
        this.jwtService = jwtService;
        this.userCache = userCache;
        this.sessionCookieService = sessionCookieService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // PAT/OAuth MCP já autenticados têm precedência sobre a sessão cookie.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = sessionCookieService.extractToken(request).orElse(null);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validar token JWT
        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extrair userId do token
        UUID userId = jwtService.extractUserId(token);

        // Buscar usuário (cacheado com TTL curto para evitar lookup por request)
        User user = userCache.findById(userId).orElse(null);

        // Se usuário não existe mais, continuar sem autenticar
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Criar autenticação com authorities derivadas do role do usuário
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                user,
                null,
                authoritiesFor(user)
            );

        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
        );

        // Setar autenticação no SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Continuar cadeia de filtros
        filterChain.doFilter(request, response);
    }

    /**
     * Re-executa a autenticação também no dispatch ASYNC, não só no REQUEST inicial.
     *
     * <p>Por padrão, {@link OncePerRequestFilter} pula dispatches async
     * ({@code shouldNotFilterAsyncDispatch() == true}). O transporte MCP Streamable
     * HTTP completa a resposta via {@code AsyncContext.dispatch()}, que redispara o
     * {@code FilterChainProxy} inteiro (ver docs/DECISIONS.md D-023). Nesse segundo
     * passo, como esta app é STATELESS (a identidade vem do header {@code Authorization}
     * a cada requisição, não de sessão), pular a autenticação deixa o
     * {@code SecurityContext} vazio — a requisição vira anônima e o
     * {@code AuthorizationFilter} (que roda em todos os dispatch types) a nega com
     * {@code AuthorizationDeniedException} sobre uma resposta SSE já commitada,
     * virando 500 + conexão cortada. Re-autenticar no async (o header ainda está
     * presente no mesmo request) restaura o contexto e o streaming completa.</p>
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * Constrói as authorities do usuário a partir do seu role.
     *
     * <p>O prefixo {@code ROLE_} é o convencionado pelo Spring Security para que
     * {@code hasRole('ADMIN')} / {@code @PreAuthorize("hasRole('ADMIN')")}
     * funcionem. Usuários sem role definido recebem {@code ROLE_USER}.</p>
     *
     * @param user usuário autenticado
     * @return lista imutável com a authority correspondente ao role
     */
    private List<GrantedAuthority> authoritiesFor(User user) {
        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
