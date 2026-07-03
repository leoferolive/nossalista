package br.com.leoferolive.nossalista.mcpoauth.security;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.config.AuthenticatedUserCache;
import br.com.leoferolive.nossalista.config.PatAuthorizationSupport;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthJwtService;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthJwtService.McpOAuthAccessTokenClaims;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (mocks) de {@link McpOAuthTokenAuthenticationFilter}. Chama
 * {@code doFilter} (herdado de {@code OncePerRequestFilter}) diretamente, mesmo
 * padrão de {@code JwtAuthenticationFilterTest}. Complementa
 * {@code McpOAuthFlowIntegrationTest} nos ramos que exigem um token
 * sintaticamente válido mas semanticamente "quase certo" (esquema de auth
 * errado, usuário do claim que não existe mais, role nula).
 */
@ExtendWith(MockitoExtension.class)
class McpOAuthTokenAuthenticationFilterTest {

    @Mock
    private McpOAuthJwtService jwtService;

    @Mock
    private AuthenticatedUserCache userCache;

    private McpOAuthTokenAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new McpOAuthTokenAuthenticationFilter(jwtService, userCache);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private HttpServletRequest requestWithHeader(String authorizationHeader) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/mcp");
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        return request;
    }

    @Test
    @DisplayName("esquema de autenticação diferente de Bearer segue a cadeia sem autenticar")
    void nonBearerAuthorizationHeaderPassesThrough() throws Exception {
        HttpServletRequest request = requestWithHeader("Basic dXNlcjpwYXNz");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("claims válidos mas usuário não encontrado no cache seguem a cadeia sem autenticar")
    void validClaimsButUnknownUserPassesThrough() throws Exception {
        UUID userId = UUID.randomUUID();
        HttpServletRequest request = requestWithHeader("Bearer some.jwt.token");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.validate("some.jwt.token"))
            .thenReturn(Optional.of(new McpOAuthAccessTokenClaims(userId, "claude-code", TokenScope.READ)));
        when(userCache.findById(userId)).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("usuário autenticado sem role definida recai em ROLE_USER (mesma regra do JwtAuthenticationFilter)")
    void userWithNullRoleFallsBackToRoleUser() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("oauth-user");
        user.setRole(null);

        HttpServletRequest request = requestWithHeader("Bearer some.jwt.token");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.validate("some.jwt.token"))
            .thenReturn(Optional.of(new McpOAuthAccessTokenClaims(userId, "claude-code", TokenScope.READ_WRITE)));
        when(userCache.findById(userId)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
            .extracting(Object::toString)
            .containsExactlyInAnyOrder(
                "ROLE_USER", PatAuthorizationSupport.PAT_AUTHORITY, PatAuthorizationSupport.MCP_OAUTH_AUTHORITY,
                TokenScope.READ_WRITE.authority());
        verify(chain).doFilter(request, response);
    }
}
