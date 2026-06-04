package br.com.leoferolive.nossalista.email.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("ConsoleEmailService")
class ConsoleEmailServiceTest {

    private ConsoleEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new ConsoleEmailService("http://localhost:5173");
    }

    @Test
    @DisplayName("sendPasswordReset não deve lançar exceção")
    void sendPasswordResetDoesNotThrow() {
        assertThatCode(() ->
            emailService.sendPasswordReset("user@test.com", "João", "token-123"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendEmailVerification não deve lançar exceção")
    void sendEmailVerificationDoesNotThrow() {
        assertThatCode(() ->
            emailService.sendEmailVerification("user@test.com", "Maria", "verify-456"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendMagicLink não deve lançar exceção")
    void sendMagicLinkDoesNotThrow() {
        assertThatCode(() ->
            emailService.sendMagicLink("user@test.com", "Pedro", "magic-789"))
            .doesNotThrowAnyException();
    }
}
