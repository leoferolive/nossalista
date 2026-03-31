package br.com.leoferolive.nossalista.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Implementação de envio de e-mail via SMTP (Brevo).
 * Ativada quando app.email.smtp.enabled=true. Tem prioridade sobre ConsoleEmailService.
 */
@Service
@Primary
@ConditionalOnProperty(name = "app.email.smtp.enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);
    private static final int RESET_EXPIRATION_MINUTES = 60;
    private static final int MAGIC_LINK_EXPIRATION_MINUTES = 10;

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final String fromEmail;
    private final String fromName;
    private final String frontendUrl;

    public SmtpEmailService(
        JavaMailSender mailSender,
        ITemplateEngine templateEngine,
        @Value("${app.email.from}") String fromEmail,
        @Value("${app.email.from-name}") String fromName,
        @Value("${frontend.url}") String frontendUrl
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void sendPasswordReset(String toEmail, String userName, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("actionUrl", resetLink);
        context.setVariable("expirationMinutes", RESET_EXPIRATION_MINUTES);

        String html = templateEngine.process("email/password-reset", context);
        sendHtmlEmail(toEmail, "Redefinir sua senha — NossaLista", html);

        log.info("Password reset email sent to: {}", maskEmail(toEmail));
    }

    @Override
    public void sendEmailVerification(String toEmail, String userName, String verificationToken) {
        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("actionUrl", verificationLink);

        String html = templateEngine.process("email/email-verification", context);
        sendHtmlEmail(toEmail, "Confirme seu e-mail — NossaLista", html);

        log.info("Email verification sent to: {}", maskEmail(toEmail));
    }

    @Override
    public void sendMagicLink(String toEmail, String userName, String loginToken) {
        String loginLink = frontendUrl + "/magic-login?token=" + loginToken;

        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("actionUrl", loginLink);
        context.setVariable("expirationMinutes", MAGIC_LINK_EXPIRATION_MINUTES);

        String html = templateEngine.process("email/magic-link", context);
        sendHtmlEmail(toEmail, "Seu link de acesso — NossaLista", html);

        log.info("Magic link email sent to: {}", maskEmail(toEmail));
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to: {}", maskEmail(to), e);
            throw new EmailSendException("Erro ao enviar e-mail", e);
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex < 0) {
            return "***";
        }
        String domain = email.substring(atIndex + 1);
        int dotIndex = domain.lastIndexOf('.');
        String maskedDomain = dotIndex > 1
            ? domain.charAt(0) + "***" + domain.substring(dotIndex)
            : "***";
        if (atIndex <= 1) {
            return "***@" + maskedDomain;
        }
        return email.charAt(0) + "***@" + maskedDomain;
    }
}
