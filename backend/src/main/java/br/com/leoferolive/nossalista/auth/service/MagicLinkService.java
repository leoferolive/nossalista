package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.MagicLinkToken;
import br.com.leoferolive.nossalista.auth.exception.InvalidMagicLinkTokenException;
import br.com.leoferolive.nossalista.auth.repository.MagicLinkTokenRepository;
import br.com.leoferolive.nossalista.email.service.EmailService;
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
 * Serviço para login por magic link.
 *
 * <p>Gera um token opaco de uso único (validade 10 min), dispara o e-mail via
 * {@link EmailService} (template {@code magic-link.html}) e, no consumo, valida o
 * token, marca o e-mail como verificado (posse comprovada) e devolve o usuário
 * para o controller emitir o JWT. Só loga contas já existentes — e-mail
 * inexistente é no-op silencioso (anti-enumeração).</p>
 */
@Service
public class MagicLinkService {

    private static final Logger log = LoggerFactory.getLogger(MagicLinkService.class);

    /** Validade do token. DEVE coincidir com SmtpEmailService.MAGIC_LINK_EXPIRATION_MINUTES. */
    private static final int MAGIC_LINK_EXPIRATION_MINUTES = 10;

    private final UserService userService;
    private final MagicLinkTokenRepository tokenRepository;
    private final EmailService emailService;

    public MagicLinkService(
        UserService userService,
        MagicLinkTokenRepository tokenRepository,
        EmailService emailService
    ) {
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    /**
     * Solicita um magic link para o e-mail. No-op silencioso se a conta não existe
     * (anti-enumeração). Funciona para qualquer AuthProvider (inclui Google).
     */
    @Transactional
    public void requestMagicLink(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> userOpt = userService.findByEmailOptional(normalizedEmail);
        if (userOpt.isEmpty()) {
            log.debug("Magic link requested for non-existent email: {}", normalizedEmail);
            return;
        }

        User user = userOpt.get();
        tokenRepository.deleteByUserIdAndUsedFalse(user.getId());

        String tokenValue = UUID.randomUUID().toString();
        MagicLinkToken token = new MagicLinkToken();
        token.setUserId(user.getId());
        token.setToken(tokenValue);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(MAGIC_LINK_EXPIRATION_MINUTES));
        tokenRepository.save(token);

        try {
            emailService.sendMagicLink(user.getEmail(), user.getName(), tokenValue);
        } catch (Exception e) {
            log.error("Failed to send magic link email to {}: {}", normalizedEmail, e.getMessage());
        }
    }

    /**
     * Valida e consome um token de magic link. Marca o e-mail como verificado e
     * retorna o usuário para o controller emitir o JWT.
     *
     * @throws InvalidMagicLinkTokenException se o token for inválido, expirado ou já usado
     */
    @Transactional
    public User consume(String token) {
        MagicLinkToken magicToken = tokenRepository.findByTokenAndUsedFalse(token)
            .orElseThrow(() -> new InvalidMagicLinkTokenException("Token inválido ou já utilizado"));

        if (magicToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidMagicLinkTokenException("Token expirado");
        }

        magicToken.setUsed(true);
        tokenRepository.save(magicToken);

        userService.markEmailVerified(magicToken.getUserId());

        return userService.findById(magicToken.getUserId())
            .orElseThrow(() -> new InvalidMagicLinkTokenException("Usuário não encontrado"));
    }
}
