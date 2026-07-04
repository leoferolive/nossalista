package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.MagicLinkToken;
import br.com.leoferolive.nossalista.auth.exception.InvalidMagicLinkTokenException;
import br.com.leoferolive.nossalista.auth.repository.MagicLinkTokenRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
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
@DisplayName("MagicLinkService")
class MagicLinkServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private MagicLinkTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    private MagicLinkService magicLinkService;

    @BeforeEach
    void setUp() {
        magicLinkService = new MagicLinkService(userService, tokenRepository, emailService);
    }

    private User user(AuthProvider provider) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("ana@test.com");
        u.setName("Ana");
        u.setAuthProvider(provider);
        return u;
    }

    @Test
    @DisplayName("requestMagicLink envia e-mail e cria token quando usuário existe")
    void requestSendsWhenUserExists() {
        User u = user(AuthProvider.EMAIL);
        when(userService.findByEmailOptional("ana@test.com")).thenReturn(Optional.of(u));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        magicLinkService.requestMagicLink("ana@test.com");

        verify(tokenRepository).deleteByUserIdAndUsedFalse(u.getId());
        verify(tokenRepository).save(any(MagicLinkToken.class));
        verify(emailService).sendMagicLink(eq("ana@test.com"), eq("Ana"), anyString());
    }

    @Test
    @DisplayName("requestMagicLink funciona para conta OAuth (não filtra provider)")
    void requestWorksForOAuthAccount() {
        User u = user(AuthProvider.GOOGLE);
        when(userService.findByEmailOptional("ana@test.com")).thenReturn(Optional.of(u));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        magicLinkService.requestMagicLink("ana@test.com");

        verify(emailService).sendMagicLink(eq("ana@test.com"), eq("Ana"), anyString());
    }

    @Test
    @DisplayName("requestMagicLink é no-op quando usuário não existe")
    void requestNoOpWhenUserMissing() {
        when(userService.findByEmailOptional("nao@test.com")).thenReturn(Optional.empty());

        magicLinkService.requestMagicLink("nao@test.com");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendMagicLink(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("requestMagicLink não propaga falha de envio de e-mail")
    void requestDoesNotPropagateEmailFailure() {
        User u = user(AuthProvider.EMAIL);
        when(userService.findByEmailOptional("ana@test.com")).thenReturn(Optional.of(u));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("SMTP down"))
            .when(emailService).sendMagicLink(anyString(), anyString(), anyString());

        assertThatCode(() -> magicLinkService.requestMagicLink("ana@test.com"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("consume válido marca token usado, verifica e-mail e retorna o usuário")
    void consumeValid() {
        UUID userId = UUID.randomUUID();
        MagicLinkToken token = new MagicLinkToken();
        token.setUserId(userId);
        token.setToken("valid");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        token.setUsed(false);
        User u = new User();
        u.setId(userId);
        u.setEmail("ana@test.com");

        when(tokenRepository.findByTokenAndUsedFalse("valid")).thenReturn(Optional.of(token));
        when(userService.findById(userId)).thenReturn(Optional.of(u));

        User result = magicLinkService.consume("valid");

        assertThat(token.isUsed()).isTrue();
        verify(tokenRepository).save(token);
        verify(userService).markEmailVerified(userId);
        assertThat(result).isEqualTo(u);
    }

    @Test
    @DisplayName("consume lança para token inválido")
    void consumeInvalid() {
        when(tokenRepository.findByTokenAndUsedFalse("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> magicLinkService.consume("x"))
            .isInstanceOf(InvalidMagicLinkTokenException.class);
    }

    @Test
    @DisplayName("consume lança para token expirado")
    void consumeExpired() {
        MagicLinkToken token = new MagicLinkToken();
        token.setUserId(UUID.randomUUID());
        token.setToken("old");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        token.setUsed(false);
        when(tokenRepository.findByTokenAndUsedFalse("old")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> magicLinkService.consume("old"))
            .isInstanceOf(InvalidMagicLinkTokenException.class);
        verify(userService, never()).markEmailVerified(any());
    }
}
