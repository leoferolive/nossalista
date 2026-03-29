package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.PasswordResetToken;
import br.com.leoferolive.nossalista.auth.exception.InvalidResetTokenException;
import br.com.leoferolive.nossalista.auth.repository.PasswordResetTokenRepository;
import br.com.leoferolive.nossalista.email.service.EmailService;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para operações de reset de senha.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserService userService;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(
        UserService userService,
        PasswordResetTokenRepository tokenRepository,
        PasswordEncoder passwordEncoder,
        EmailService emailService
    ) {
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Solicita reset de senha para um email.
     * Não lança erro se email não existir (previne enumeração de emails).
     *
     * @param email email do usuário
     */
    @Transactional
    public void requestReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> userOpt = userService.findByEmailOptional(normalizedEmail);
        if (userOpt.isEmpty()) {
            log.debug("Password reset requested for non-existent email: {}", normalizedEmail);
            return;
        }

        User user = userOpt.get();

        // Limpar tokens antigos não usados do mesmo usuário
        tokenRepository.deleteByUserIdAndUsedFalse(user.getId());

        // Criar novo token com expiração de 1 hora
        String tokenValue = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(tokenValue);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));

        tokenRepository.save(resetToken);

        emailService.sendPasswordReset(user.getEmail(), user.getName(), tokenValue);
    }

    /**
     * Redefine a senha usando um token válido.
     *
     * @param token       token de reset
     * @param newPassword nova senha (texto puro)
     * @throws InvalidResetTokenException se token inválido, expirado ou já usado
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token)
            .orElseThrow(() -> new InvalidResetTokenException("Token inválido ou já utilizado"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidResetTokenException("Token expirado");
        }

        // Marcar token como usado
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        // Atualizar senha do usuário
        userService.updatePassword(resetToken.getUserId(), passwordEncoder.encode(newPassword));
    }
}
