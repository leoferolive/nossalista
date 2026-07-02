package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.apitoken.domain.PersonalAccessToken;
import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PersonalAccessTokenAuthenticationFilter")
class PersonalAccessTokenAuthenticationFilterTest {

    private static final String VALID_PAT = "nlmcp_" + "a".repeat(64);
    private static final UUID USER_ID = UUID.randomUUID();

    private PersonalAccessTokenService tokenService;
    private UserService userService;
    private RateLimiterService rateLimiterService;
    private ClientIpResolver clientIpResolver;
    private PersonalAccessTokenAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        tokenService = mock(PersonalAccessTokenService.class);
        userService = mock(UserService.class);
        rateLimiterService = new RateLimiterService();
        clientIpResolver = mock(ClientIpResolver.class);
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("203.0.113.9");

        AuthenticatedUserCache cache = new AuthenticatedUserCache(userService, Duration.ofSeconds(60));
        filter = new PersonalAccessTokenAuthenticationFilter(
            tokenService, cache, rateLimiterService, clientIpResolver);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private HttpServletRequest requestWithBearer(String token) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(token == null ? null : "Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/lists");
        return request;
    }

    private User userWithRole(Role role) {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("mcp-user");
        user.setRole(role);
        return user;
    }

    @Test
    @DisplayName("ignora tokens sem o prefixo nlmcp_ e deixa a cadeia continuar sem autenticar")
    void ignoresNonPatTokens() throws Exception {
        HttpServletRequest request = requestWithBearer("some.jwt.token");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("ignora requisições sem header Authorization")
    void ignoresMissingAuthHeader() throws Exception {
        HttpServletRequest request = requestWithBearer(null);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("PAT válido autentica com ROLE do usuário, PAT_AUTH e authority de escopo")
    void validPatAuthenticatesWithExpectedAuthorities() throws Exception {
        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setUserId(USER_ID);
        pat.setScope(TokenScope.READ);
        when(tokenService.authenticate(VALID_PAT)).thenReturn(Optional.of(pat));
        when(userService.findById(USER_ID)).thenReturn(Optional.of(userWithRole(Role.USER)));

        HttpServletRequest request = requestWithBearer(VALID_PAT);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
            .extracting(Object::toString)
            .containsExactlyInAnyOrder("ROLE_USER", "PAT_AUTH", "SCOPE_READ");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("PAT com escopo READ_WRITE recebe authority SCOPE_READ_WRITE")
    void readWriteScopeGetsCorrectAuthority() throws Exception {
        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setUserId(USER_ID);
        pat.setScope(TokenScope.READ_WRITE);
        when(tokenService.authenticate(VALID_PAT)).thenReturn(Optional.of(pat));
        when(userService.findById(USER_ID)).thenReturn(Optional.of(userWithRole(Role.USER)));

        filter.doFilter(requestWithBearer(VALID_PAT), mock(HttpServletResponse.class), mock(FilterChain.class));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities()).extracting(Object::toString).contains("SCOPE_READ_WRITE");
    }

    @Test
    @DisplayName("PAT inválido não autentica e segue a cadeia (401 é responsabilidade do endpoint)")
    void invalidPatDoesNotAuthenticate() throws Exception {
        when(tokenService.authenticate(VALID_PAT)).thenReturn(Optional.empty());

        HttpServletRequest request = requestWithBearer(VALID_PAT);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("usuário do PAT não existe mais: não autentica")
    void patUserNoLongerExistsDoesNotAuthenticate() throws Exception {
        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setUserId(USER_ID);
        pat.setScope(TokenScope.READ);
        when(tokenService.authenticate(VALID_PAT)).thenReturn(Optional.of(pat));
        when(userService.findById(USER_ID)).thenReturn(Optional.empty());

        HttpServletRequest request = requestWithBearer(VALID_PAT);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("excede limite de tentativas inválidas por IP: responde 429 e não continua a cadeia")
    void exceedsInvalidAttemptsRateLimit() throws Exception {
        when(tokenService.authenticate(VALID_PAT)).thenReturn(Optional.empty());

        StringWriter body = new StringWriter();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < PersonalAccessTokenAuthenticationFilter.INVALID_ATTEMPTS_LIMIT; i++) {
            filter.doFilter(requestWithBearer(VALID_PAT), mock(HttpServletResponse.class), mock(FilterChain.class));
        }

        // Próxima tentativa excede o limite: deve responder 429 sem chamar a cadeia.
        filter.doFilter(requestWithBearer(VALID_PAT), response, chain);

        verify(response).setStatus(429);
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(response));
        assertThat(body.toString()).contains("Muitas tentativas");
    }

    @Test
    @DisplayName("IP já bloqueado: responde 429 sem fazer lookup do token no banco")
    void blockedIpSkipsTokenLookupEntirely() throws Exception {
        when(tokenService.authenticate(VALID_PAT)).thenReturn(Optional.empty());

        for (int i = 0; i < PersonalAccessTokenAuthenticationFilter.INVALID_ATTEMPTS_LIMIT; i++) {
            filter.doFilter(requestWithBearer(VALID_PAT), mock(HttpServletResponse.class), mock(FilterChain.class));
        }
        verify(tokenService, org.mockito.Mockito.times(PersonalAccessTokenAuthenticationFilter.INVALID_ATTEMPTS_LIMIT))
            .authenticate(VALID_PAT);

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        filter.doFilter(requestWithBearer(VALID_PAT), response, mock(FilterChain.class));

        // A requisição que estoura o limite é bloqueada ANTES do lookup — a
        // contagem de chamadas a authenticate() não deve mudar.
        verify(tokenService, org.mockito.Mockito.times(PersonalAccessTokenAuthenticationFilter.INVALID_ATTEMPTS_LIMIT))
            .authenticate(VALID_PAT);
        verify(response).setStatus(429);
    }
}
