package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.EmailVerificationToken;
import br.com.leoferolive.nossalista.auth.exception.InvalidVerificationTokenException;
import br.com.leoferolive.nossalista.auth.repository.EmailVerificationTokenRepository;
import br.com.leoferolive.nossalista.email.service.EmailService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService (Q2.7)")
class EmailVerificationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(userService, tokenRepository, emailService);
    }

    private User user(AuthProvider provider, boolean verified) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("joao@test.com");
        user.setName("João");
        user.setAuthProvider(provider);
        user.setEmailVerified(verified);
        return user;
    }

    @Test
    @DisplayName("sendVerification cria token e envia e-mail")
    void sendVerificationCreatesTokenAndSendsEmail() {
        User user = user(AuthProvider.EMAIL, false);
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.sendVerification(user);

        verify(tokenRepository).deleteByUserIdAndUsedFalse(user.getId());
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendEmailVerification(eq("joao@test.com"), eq("João"), anyString());
    }

    @Test
    @DisplayName("sendVerification não propaga exceção quando envio de e-mail falha")
    void sendVerificationDoesNotPropagateEmailFailure() {
        User user = user(AuthProvider.EMAIL, false);
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP down"))
            .when(emailService).sendEmailVerification(anyString(), anyString(), anyString());

        assertThatCode(() -> service.sendVerification(user)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("verify marca e-mail verificado e consome o token")
    void verifyMarksVerifiedAndConsumesToken() {
        UUID userId = UUID.randomUUID();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(userId);
        token.setToken("valid-token");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(false);

        when(tokenRepository.findByTokenAndUsedFalse("valid-token")).thenReturn(Optional.of(token));

        service.verify("valid-token");

        verify(userService).markEmailVerified(userId);
        verify(tokenRepository).save(token);
        org.assertj.core.api.Assertions.assertThat(token.isUsed()).isTrue();
    }

    @Test
    @DisplayName("verify lança exceção para token inválido")
    void verifyThrowsForInvalidToken() {
        when(tokenRepository.findByTokenAndUsedFalse("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("bad"))
            .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    @DisplayName("verify lança exceção para token expirado")
    void verifyThrowsForExpiredToken() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(UUID.randomUUID());
        token.setToken("expired");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        token.setUsed(false);

        when(tokenRepository.findByTokenAndUsedFalse("expired")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verify("expired"))
            .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    @DisplayName("resendVerification reenvia para conta EMAIL não verificada")
    void resendSendsForUnverifiedEmailUser() {
        User user = user(AuthProvider.EMAIL, false);
        when(userService.findByEmailOptional("joao@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resendVerification("joao@test.com");

        verify(emailService).sendEmailVerification(eq("joao@test.com"), eq("João"), anyString());
    }

    @Test
    @DisplayName("resendVerification não reenvia para conta já verificada")
    void resendSkipsVerifiedUser() {
        User user = user(AuthProvider.EMAIL, true);
        when(userService.findByEmailOptional("joao@test.com")).thenReturn(Optional.of(user));

        service.resendVerification("joao@test.com");

        verify(emailService, never()).sendEmailVerification(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("resendVerification não reenvia para conta OAuth (Google)")
    void resendSkipsOauthUser() {
        User user = user(AuthProvider.GOOGLE, true);
        when(userService.findByEmailOptional("joao@test.com")).thenReturn(Optional.of(user));

        service.resendVerification("joao@test.com");

        verify(emailService, never()).sendEmailVerification(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("resendVerification não reenvia para e-mail inexistente (anti-enumeração)")
    void resendSkipsUnknownEmail() {
        when(userService.findByEmailOptional("nobody@test.com")).thenReturn(Optional.empty());

        service.resendVerification("nobody@test.com");

        verify(emailService, never()).sendEmailVerification(anyString(), anyString(), anyString());
    }
}
