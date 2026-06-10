package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.EmailVerificationToken;
import br.com.leoferolive.nossalista.auth.exception.InvalidVerificationTokenException;
import br.com.leoferolive.nossalista.auth.repository.EmailVerificationTokenRepository;
import br.com.leoferolive.nossalista.email.service.EmailService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para operações de verificação de e-mail (Q2.7).
 *
 * <p>Gera tokens de verificação, dispara o e-mail via {@link EmailService}
 * (template {@code email-verification.html}), valida/consome o token e marca o
 * usuário como verificado. O envio de e-mail é best-effort: uma falha de SMTP
 * não deve quebrar o registro do usuário.</p>
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    /** Validade do token de verificação. */
    private static final int TOKEN_EXPIRATION_HOURS = 24;

    private final UserService userService;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    public EmailVerificationService(
        UserService userService,
        EmailVerificationTokenRepository tokenRepository,
        EmailService emailService
    ) {
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    /**
     * Gera um token e envia o e-mail de verificação para o usuário informado.
     * Falhas de envio são logadas mas não propagadas (não quebram o registro).
     *
     * @param user usuário recém-registrado (ou que solicitou reenvio)
     */
    @Transactional
    public void sendVerification(User user) {
        // Limpar tokens antigos não usados do mesmo usuário
        tokenRepository.deleteByUserIdAndUsedFalse(user.getId());

        String tokenValue = UUID.randomUUID().toString();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setToken(tokenValue);
        token.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS));
        tokenRepository.save(token);

        try {
            emailService.sendEmailVerification(user.getEmail(), user.getName(), tokenValue);
        } catch (Exception e) {
            log.error("Failed to send email verification to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Valida e consome um token de verificação, marcando o e-mail como verificado.
     *
     * @param token token de verificação
     * @throws InvalidVerificationTokenException se token inválido, expirado ou já usado
     */
    @Transactional
    public void verify(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByTokenAndUsedFalse(token)
            .orElseThrow(() -> new InvalidVerificationTokenException("Token inválido ou já utilizado"));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidVerificationTokenException("Token expirado");
        }

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        userService.markEmailVerified(verificationToken.getUserId());
    }

    /**
     * Reenvia o e-mail de verificação para um endereço.
     *
     * <p>Não revela se o e-mail existe (previne enumeração) e não reenvia para
     * contas OAuth ou já verificadas — nesses casos é um no-op silencioso.</p>
     *
     * @param email e-mail do usuário
     */
    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userService.findByEmailOptional(normalizedEmail);

        if (userOpt.isEmpty()) {
            log.debug("Email verification resend requested for non-existent email: {}", normalizedEmail);
            return;
        }

        User user = userOpt.get();
        if (user.getAuthProvider() != AuthProvider.EMAIL || user.isEmailVerified()) {
            log.debug("Email verification resend skipped (provider={}, verified={}) for {}",
                user.getAuthProvider(), user.isEmailVerified(), normalizedEmail);
            return;
        }

        sendVerification(user);
    }
}
