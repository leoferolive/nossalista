package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.auth.service.JwtService;
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

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserService userService;
    private JwtAuthenticationFilter filter;

    private final UUID userId = UUID.randomUUID();
    private static final String TOKEN = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userService = mock(UserService.class);
        AuthenticatedUserCache cache =
            new AuthenticatedUserCache(userService, Duration.ofSeconds(60));
        filter = new JwtAuthenticationFilter(jwtService, cache);

        when(jwtService.validateToken(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication runFilterFor(User user) throws Exception {
        when(userService.findById(userId)).thenReturn(Optional.of(user));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);

        filter.doFilter(request, response, chain);
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private User userWithRole(Role role) {
        User user = new User();
        user.setId(userId);
        user.setUsername("user");
        user.setRole(role);
        return user;
    }

    @Test
    @DisplayName("usuário USER recebe authority ROLE_USER")
    void userRoleGetsRoleUserAuthority() throws Exception {
        Authentication auth = runFilterFor(userWithRole(Role.USER));

        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
            .extracting(Object::toString)
            .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("usuário ADMIN recebe authority ROLE_ADMIN")
    void adminRoleGetsRoleAdminAuthority() throws Exception {
        Authentication auth = runFilterFor(userWithRole(Role.ADMIN));

        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
            .extracting(Object::toString)
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("usuário sem role definido recai em ROLE_USER")
    void nullRoleFallsBackToRoleUser() throws Exception {
        Authentication auth = runFilterFor(userWithRole(null));

        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
            .extracting(Object::toString)
            .containsExactly("ROLE_USER");
    }
}
