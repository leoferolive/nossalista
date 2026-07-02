package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.apitoken.domain.PersonalAccessToken;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Filtro de autenticação via Personal Access Token (PAT).
 *
 * <p>Executa ANTES de {@link JwtAuthenticationFilter} (ver
 * {@code SecurityConfig}). Só age quando o header {@code Authorization}
 * carrega um token com o prefixo {@link PersonalAccessTokenService#TOKEN_PREFIX};
 * qualquer outro valor (incluindo JWTs normais) é ignorado e segue intocado
 * para o {@link JwtAuthenticationFilter}, que continua funcionando sem
 * alterações.</p>
 *
 * <p>Quando o token é válido, popula o mesmo {@code SecurityContext} que o
 * JWT popula (mesmo principal {@link User}, mesmas authorities {@code ROLE_*}),
 * acrescentando {@link PatAuthorizationSupport#PAT_AUTHORITY} e a authority de
 * escopo do token — usadas por {@code SecurityConfig} para restringir PATs de
 * escopo READ a métodos seguros e para bloquear PATs de endpoints de
 * autenticação/gestão de tokens.</p>
 *
 * <p>Antes de qualquer lookup no banco, verifica (sem incrementar) se o IP já
 * está bloqueado por tentativas inválidas anteriores — se estiver, responde
 * 429 diretamente (RFC 7807) sem tocar o banco. Só depois disso resolve o
 * token; se o lookup falhar (inválido/expirado/revogado), a tentativa é
 * registrada via {@link RateLimiterService} para contar no bloqueio das
 * próximas requisições desse IP. Um token válido nunca é contado como
 * tentativa — proteção contra força bruta do segredo do token.</p>
 */
@Component
public class PersonalAccessTokenAuthenticationFilter extends OncePerRequestFilter {

    static final int INVALID_ATTEMPTS_LIMIT = 20;
    static final Duration INVALID_ATTEMPTS_WINDOW = Duration.ofMinutes(5);

    private final PersonalAccessTokenService tokenService;
    private final AuthenticatedUserCache userCache;
    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PersonalAccessTokenAuthenticationFilter(
        PersonalAccessTokenService tokenService,
        AuthenticatedUserCache userCache,
        RateLimiterService rateLimiterService,
        ClientIpResolver clientIpResolver
    ) {
        this.tokenService = tokenService;
        this.userCache = userCache;
        this.rateLimiterService = rateLimiterService;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String candidate = authHeader.substring(7);
        if (!candidate.startsWith(PersonalAccessTokenService.TOKEN_PREFIX)) {
            // Não é um PAT (provavelmente um JWT) — deixa para o JwtAuthenticationFilter.
            filterChain.doFilter(request, response);
            return;
        }

        String rateLimitKey = "pat-auth:invalid:ip:" + clientIpResolver.resolve(request);
        if (rateLimiterService.isBlocked(rateLimitKey, INVALID_ATTEMPTS_LIMIT)) {
            respondTooManyRequests(request, response);
            return;
        }

        Optional<PersonalAccessToken> resolved = tokenService.authenticate(candidate);
        if (resolved.isEmpty()) {
            // Registra a tentativa inválida; o bloqueio (429) só passa a valer a
            // partir da PRÓXIMA requisição desse IP, verificado no isBlocked acima.
            rateLimiterService.isAllowed(rateLimitKey, INVALID_ATTEMPTS_LIMIT, INVALID_ATTEMPTS_WINDOW);
            filterChain.doFilter(request, response);
            return;
        }

        PersonalAccessToken token = resolved.get();
        User user = userCache.findById(token.getUserId()).orElse(null);
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, authoritiesFor(user, token));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void respondTooManyRequests(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            "Muitas tentativas de autenticação com token inválido. Tente novamente mais tarde."
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/rate-limit-exceeded"));
        problem.setTitle("Muitas requisições");
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }

    private List<GrantedAuthority> authoritiesFor(User user, PersonalAccessToken token) {
        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        authorities.add(new SimpleGrantedAuthority(PatAuthorizationSupport.PAT_AUTHORITY));
        authorities.add(new SimpleGrantedAuthority(token.getScope().authority()));
        return authorities;
    }
}
