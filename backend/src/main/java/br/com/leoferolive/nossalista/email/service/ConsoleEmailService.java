package br.com.leoferolive.nossalista.email.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementação de e-mail que loga no console.
 * Usado como fallback; SmtpEmailService tem @Primary quando ativado.
 */
@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    private final String frontendUrl;

    public ConsoleEmailService(@Value("${frontend.url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void sendPasswordReset(String toEmail, String userName, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        log.info("[EMAIL] Password reset → to={}, user={}, link={}", toEmail, userName, resetLink);
    }

    @Override
    public void sendEmailVerification(String toEmail, String userName, String verificationToken) {
        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
        log.info("[EMAIL] Email verification → to={}, user={}, link={}",
            toEmail, userName, verificationLink);
    }

    @Override
    public void sendMagicLink(String toEmail, String userName, String loginToken) {
        String loginLink = frontendUrl + "/magic-login?token=" + loginToken;
        log.info("[EMAIL] Magic link → to={}, user={}, link={}", toEmail, userName, loginLink);
    }
}
