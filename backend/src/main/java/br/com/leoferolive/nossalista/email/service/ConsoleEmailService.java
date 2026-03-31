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
        log.info("[EMAIL] Password reset → to={}, user={}, token={}…",
            toEmail, userName, maskToken(resetToken));
    }

    @Override
    public void sendEmailVerification(String toEmail, String userName, String verificationToken) {
        log.info("[EMAIL] Email verification → to={}, user={}, token={}…",
            toEmail, userName, maskToken(verificationToken));
    }

    @Override
    public void sendMagicLink(String toEmail, String userName, String loginToken) {
        log.info("[EMAIL] Magic link → to={}, user={}, token={}…",
            toEmail, userName, maskToken(loginToken));
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8) + "***";
    }
}
