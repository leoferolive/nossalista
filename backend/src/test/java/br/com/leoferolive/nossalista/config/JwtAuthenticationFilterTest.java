package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.auth.service.SessionCookieService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import br.com.leoferolive.nossalista.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "valid.jwt.token";

    @Autowired
    private UserRepository userRepository;

    private final UUID userId = UUID.randomUUID();
    private FakeJwtService jwtService;
    private FakeCachedUserService userService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new FakeJwtService(userId, 0);
        userService = new FakeCachedUserService(userRepository);
        AuthenticatedUserCache cache = new AuthenticatedUserCache(userService, Duration.ofSeconds(60));
        filter = new JwtAuthenticationFilter(
            jwtService,
            cache,
            userRepository,
            new FixedSessionCookieService(TOKEN)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication runFilterFor(User principal) throws Exception {
        persistCurrentSessionVersion(principal.getSessionVersion());
        userService.serve(principal);

        filter.doFilter(request(), new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest();
    }

    private void persistCurrentSessionVersion(int sessionVersion) {
        User sessionOwner = new User();
        sessionOwner.setId(userId);
        sessionOwner.setUsername("session-owner-" + userId);
        sessionOwner.setEmail("session-owner-" + userId + "@example.com");
        sessionOwner.setPassword("hashed-password");
        sessionOwner.setAuthProvider(AuthProvider.EMAIL);
        sessionOwner.setRole(Role.USER);
        sessionOwner.setSessionVersion(sessionVersion);
        userRepository.save(sessionOwner);
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

    @Test
    @DisplayName("versão de sessão é consultada diretamente e token revogado não autentica")
    void sessionVersionMustMatchDirectRepositoryProjection() throws Exception {
        User user = userWithRole(Role.USER);
        user.setSessionVersion(1);

        Authentication auth = runFilterFor(user);

        assertThat(auth).isNull();
    }

    @Test
    @DisplayName("JWT de sessão sem versão não autentica mesmo para versão zero")
    void missingSessionVersionFailsClosed() throws Exception {
        jwtService.setSessionVersion(null);

        Authentication auth = runFilterFor(userWithRole(Role.USER));

        assertThat(auth).isNull();
    }

    private static final class FakeJwtService extends JwtService {

        private final UUID userId;
        private Integer sessionVersion;

        private FakeJwtService(UUID userId, Integer sessionVersion) {
            this.userId = userId;
            this.sessionVersion = sessionVersion;
        }

        @Override
        public boolean validateToken(String token) {
            return TOKEN.equals(token);
        }

        @Override
        public UUID extractUserId(String token) {
            return userId;
        }

        @Override
        public Integer extractSessionVersion(String token) {
            return sessionVersion;
        }

        private void setSessionVersion(Integer sessionVersion) {
            this.sessionVersion = sessionVersion;
        }
    }

    private static final class FakeCachedUserService extends UserService {

        private User cachedUser;

        private FakeCachedUserService(UserRepository userRepository) {
            super(userRepository);
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(cachedUser);
        }

        private void serve(User user) {
            cachedUser = user;
        }
    }

    private static final class FixedSessionCookieService extends SessionCookieService {

        private final String token;

        private FixedSessionCookieService(String token) {
            super(new MockEnvironment());
            this.token = token;
        }

        @Override
        public Optional<String> extractToken(HttpServletRequest request) {
            return Optional.ofNullable(token);
        }
    }
}
