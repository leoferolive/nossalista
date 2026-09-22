package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
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
import java.util.Optional;
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
    private final UserRepository userRepository;
    private final SessionCookieService sessionCookieService;

    public JwtAuthenticationFilter(JwtService jwtService, AuthenticatedUserCache userCache,
                                   UserRepository userRepository,
                                   SessionCookieService sessionCookieService) {
        this.jwtService = jwtService;
        this.userCache = userCache;
        this.userRepository = userRepository;
        this.sessionCookieService = sessionCookieService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        authenticateSession(request);
        filterChain.doFilter(request, response);
    }

    /**
     * PAT/OAuth MCP já autenticados têm precedência sobre a sessão cookie.
     * JWTs inválidos, claims incompatíveis e usuários removidos continuam a cadeia sem autenticar.
     */
    private void authenticateSession(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        sessionCookieService.extractToken(request)
            .filter(jwtService::validateToken)
            .flatMap(this::findSessionUser)
            .ifPresent(user -> authenticateUser(user, request));
    }

    /**
     * Claims assinadas mas incompatíveis com a sessão falham fechado.
     * Busca o usuário cacheado com TTL curto para evitar lookup por request.
     */
    private Optional<User> findSessionUser(String token) {
        SessionClaims claims = extractSessionClaims(token).orElse(null);
        if (claims == null || !hasCurrentSessionVersion(claims.userId(), claims.sessionVersion())) {
            return Optional.empty();
        }
        return userCache.findById(claims.userId());
    }

    private Optional<SessionClaims> extractSessionClaims(String token) {
        try {
            return Optional.of(new SessionClaims(
                jwtService.extractUserId(token), jwtService.extractSessionVersion(token)));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private boolean hasCurrentSessionVersion(UUID userId, Integer tokenSessionVersion) {
        Integer currentSessionVersion = userRepository.findSessionVersionById(userId).orElse(null);
        return tokenSessionVersion != null
            && tokenSessionVersion.equals(currentSessionVersion);
    }

    /** Cria autenticação com authorities derivadas do role e a associa ao request atual. */
    private void authenticateUser(User user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            user, null, authoritiesFor(user));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
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

    private record SessionClaims(UUID userId, Integer sessionVersion) {
    }
}
