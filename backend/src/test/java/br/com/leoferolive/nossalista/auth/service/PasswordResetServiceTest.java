package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.PasswordResetToken;
import br.com.leoferolive.nossalista.auth.exception.InvalidResetTokenException;
import br.com.leoferolive.nossalista.auth.repository.PasswordResetTokenRepository;
import br.com.leoferolive.nossalista.email.service.EmailService;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService")
class PasswordResetServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
            userService, tokenRepository, passwordEncoder, emailService
        );
    }

    @Test
    @DisplayName("requestReset deve enviar e-mail quando usuário existe")
    void requestResetSendsEmailWhenUserExists() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("joao@test.com");
        user.setName("João");

        when(userService.findByEmailOptional("joao@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestReset("joao@test.com");

        verify(emailService).sendPasswordReset(eq("joao@test.com"), eq("João"), anyString());
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("requestReset não deve enviar e-mail quando usuário não existe")
    void requestResetDoesNotSendEmailWhenUserDoesNotExist() {
        when(userService.findByEmailOptional("inexistente@test.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("inexistente@test.com");

        verify(emailService, never()).sendPasswordReset(anyString(), anyString(), anyString());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("requestReset deve limpar tokens antigos antes de criar novo")
    void requestResetCleansOldTokens() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("maria@test.com");
        user.setName("Maria");

        when(userService.findByEmailOptional("maria@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestReset("maria@test.com");

        verify(tokenRepository).deleteByUserIdAndUsedFalse(user.getId());
    }

    @Test
    @DisplayName("resetPassword deve atualizar senha com token válido")
    void resetPasswordUpdatesPasswordWithValidToken() {
        UUID userId = UUID.randomUUID();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(userId);
        token.setToken("valid-token");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        token.setUsed(false);

        when(tokenRepository.findByTokenAndUsedFalse("valid-token"))
            .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded-password");

        passwordResetService.resetPassword("valid-token", "newPassword");

        verify(userService).updatePassword(userId, "encoded-password");
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("resetPassword deve lançar exceção para token inválido")
    void resetPasswordThrowsForInvalidToken() {
        when(tokenRepository.findByTokenAndUsedFalse("invalid-token"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("invalid-token", "newPass"))
            .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    @DisplayName("resetPassword deve lançar exceção para token expirado")
    void resetPasswordThrowsForExpiredToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(UUID.randomUUID());
        token.setToken("expired-token");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        token.setUsed(false);

        when(tokenRepository.findByTokenAndUsedFalse("expired-token"))
            .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "newPass"))
            .isInstanceOf(InvalidResetTokenException.class);
    }
}
