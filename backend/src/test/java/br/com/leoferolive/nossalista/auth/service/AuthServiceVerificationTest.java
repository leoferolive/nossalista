package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.dto.LoginRequest;
import br.com.leoferolive.nossalista.auth.dto.RegisterRequest;
import br.com.leoferolive.nossalista.auth.exception.EmailNotVerifiedException;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - verificação de e-mail (Q2.7)")
class AuthServiceVerificationTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, userRepository, passwordEncoder, emailVerificationService);
    }

    private User user(AuthProvider provider, boolean verified, String hashedPassword) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("leo@test.com");
        user.setUsername("leo");
        user.setName("Leo");
        user.setAuthProvider(provider);
        user.setEmailVerified(verified);
        user.setPassword(hashedPassword);
        return user;
    }

    @Test
    @DisplayName("register dispara envio de verificação de e-mail")
    void registerTriggersVerificationEmail() {
        RegisterRequest request = new RegisterRequest("leo@test.com", "leo", "senha123", "Leo");
        User created = user(AuthProvider.EMAIL, false, "$2a$hash");

        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hash");
        when(userService.createUser(eq("leo"), eq("leo@test.com"), eq("$2a$hash"), eq("Leo"), eq(AuthProvider.EMAIL)))
            .thenReturn(created);

        User result = authService.register(request);

        assertThat(result).isSameAs(created);
        verify(emailVerificationService).sendVerification(created);
    }

    @Test
    @DisplayName("login bloqueia conta EMAIL não-verificada quando enforcement ligado")
    void loginBlocksUnverifiedWhenEnforcementOn() {
        authService.setRequireEmailVerification(true);
        User user = user(AuthProvider.EMAIL, false, "$2a$hash");

        when(userService.findByEmailOptional("leo@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "$2a$hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("leo@test.com", "senha123")))
            .isInstanceOf(EmailNotVerifiedException.class);
    }

    @Test
    @DisplayName("login permite conta EMAIL não-verificada quando enforcement desligado")
    void loginAllowsUnverifiedWhenEnforcementOff() {
        authService.setRequireEmailVerification(false);
        User user = user(AuthProvider.EMAIL, false, "$2a$hash");

        when(userService.findByEmailOptional("leo@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "$2a$hash")).thenReturn(true);

        User result = authService.login(new LoginRequest("leo@test.com", "senha123"));

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("login permite conta EMAIL verificada com enforcement ligado")
    void loginAllowsVerifiedWhenEnforcementOn() {
        authService.setRequireEmailVerification(true);
        User user = user(AuthProvider.EMAIL, true, "$2a$hash");

        when(userService.findByEmailOptional("leo@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "$2a$hash")).thenReturn(true);

        assertThatCode(() -> authService.login(new LoginRequest("leo@test.com", "senha123")))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("login de conta OAuth (Google) nunca é bloqueado mesmo com enforcement ligado")
    void loginGoogleNeverBlocked() {
        authService.setRequireEmailVerification(true);
        // Conta Google entra já verificada; o gating só atinge provider EMAIL.
        User user = user(AuthProvider.GOOGLE, false, null);

        when(userService.findByEmailOptional("leo@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), any())).thenReturn(true);

        assertThatCode(() -> authService.login(new LoginRequest("leo@test.com", "senha123")))
            .doesNotThrowAnyException();
    }
}
