package br.com.leoferolive.nossalista.email.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.IContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmtpEmailService")
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ITemplateEngine templateEngine;

    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new SmtpEmailService(
            mailSender,
            templateEngine,
            "no-reply@nossalista.leoferolive.com.br",
            "NossaLista",
            "http://localhost:5173"
        );
    }

    @Test
    @DisplayName("sendPasswordReset deve renderizar template e enviar e-mail")
    void sendPasswordResetRendersTemplateAndSends() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doReturn("<html>reset</html>").when(templateEngine).process(anyString(), any(IContext.class));

        emailService.sendPasswordReset("user@test.com", "João", "token-123");

        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(anyString(), any(IContext.class));
    }

    @Test
    @DisplayName("sendEmailVerification deve renderizar template e enviar e-mail")
    void sendEmailVerificationRendersTemplateAndSends() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doReturn("<html>verify</html>").when(templateEngine).process(anyString(), any(IContext.class));

        emailService.sendEmailVerification("user@test.com", "Maria", "verify-456");

        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(anyString(), any(IContext.class));
    }

    @Test
    @DisplayName("sendMagicLink deve renderizar template e enviar e-mail")
    void sendMagicLinkRendersTemplateAndSends() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doReturn("<html>magic</html>").when(templateEngine).process(anyString(), any(IContext.class));

        emailService.sendMagicLink("user@test.com", "Pedro", "magic-789");

        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(anyString(), any(IContext.class));
    }

    @Test
    @DisplayName("sendPasswordReset deve propagar exceção em caso de erro")
    void sendPasswordResetThrowsOnError() {
        when(templateEngine.process(anyString(), any(IContext.class)))
            .thenReturn("<html>reset</html>");
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP down"));

        assertThatThrownBy(() ->
            emailService.sendPasswordReset("user@test.com", "João", "token"))
            .isInstanceOf(RuntimeException.class);
    }
}
