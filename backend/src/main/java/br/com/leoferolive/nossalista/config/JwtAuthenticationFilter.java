package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.auth.service.JwtService;
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
 * Filtro JWT que intercepta requests HTTP e valida tokens JWT
 * Executa uma vez por request, antes de outros filtros de autenticação
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthenticatedUserCache userCache;

    public JwtAuthenticationFilter(JwtService jwtService, AuthenticatedUserCache userCache) {
        this.jwtService = jwtService;
        this.userCache = userCache;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // Extrair header Authorization
        String authHeader = request.getHeader("Authorization");

        // Se não tem header ou não começa com "Bearer ", continuar sem autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extrair token (remover "Bearer " do início)
        String token = authHeader.substring(7);

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
