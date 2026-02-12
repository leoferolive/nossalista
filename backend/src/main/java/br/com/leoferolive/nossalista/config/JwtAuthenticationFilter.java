package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Filtro JWT que intercepta requests HTTP e valida tokens JWT
 * Executa uma vez por request, antes de outros filtros de autenticação
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
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

        // Buscar usuário no database
        User user = userService.findById(userId).orElse(null);

        // Se usuário não existe mais, continuar sem autenticar
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Criar autenticação
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                user,
                null,
                Collections.emptyList() // Authorities vazias por enquanto
            );

        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
        );

        // Setar autenticação no SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Continuar cadeia de filtros
        filterChain.doFilter(request, response);
    }
}
