package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.auth.dto.LoginRequest;
import br.com.leoferolive.nossalista.auth.dto.RegisterRequest;
import br.com.leoferolive.nossalista.auth.exception.EmailNotVerifiedException;
import br.com.leoferolive.nossalista.auth.exception.InvalidCredentialsException;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para operações de autenticação
 */
@Service
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    /**
     * Enforcement estrito de verificação de e-mail no login email/senha (Q2.7).
     * Default {@code false}: o status fica registrado mas o login não é bloqueado,
     * evitando deslogar contas pré-existentes. O dono pode ligar via
     * {@code app.auth.require-email-verification=true} quando todas as contas
     * relevantes estiverem verificadas.
     */
    @Value("${app.auth.require-email-verification:false}")
    private boolean requireEmailVerification;

    public AuthService(UserService userService, UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Setter para injeção do flag de enforcement (usado em testes).
     *
     * @param requireEmailVerification se o login deve bloquear não-verificados
     */
    void setRequireEmailVerification(boolean requireEmailVerification) {
        this.requireEmailVerification = requireEmailVerification;
    }

    /**
     * Registra um novo usuário com email e senha
     *
     * @param request dados de registro
     * @return usuário criado
     */
    @Transactional
    public User register(RegisterRequest request) {
        // Normalize and trim input
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedUsername = request.username().trim().toLowerCase();
        String trimmedPassword = request.password().trim();
        String trimmedName = request.name() != null ? request.name().trim() : null;

        // Validate email and username don't exist
        userService.validateEmailNotExists(normalizedEmail);
        userService.validateUsernameNotExists(normalizedUsername);

        // Hash password
        String hashedPassword = passwordEncoder.encode(trimmedPassword);

        // Create user via UserService
        User user = userService.createUser(
            normalizedUsername,
            normalizedEmail,
            hashedPassword,
            trimmedName,
            AuthProvider.EMAIL
        );

        // Q2.7: dispara o e-mail de verificação (best-effort, não quebra o registro)
        emailVerificationService.sendVerification(user);

        return user;
    }

    /**
     * Autentica um usuário com email e senha
     *
     * @param request credenciais de login
     * @return usuário autenticado
     * @throws InvalidCredentialsException se email não existe OU senha é incorreta
     */
    @Transactional(readOnly = true)
    public User login(LoginRequest request) {
        // Normalize email (trim + toLowerCase) igual ao registro
        String normalizedEmail = request.email().trim().toLowerCase();
        String password = request.password().trim();

        // Buscar usuário por email
        User user = userService.findByEmailOptional(normalizedEmail)
            .orElseThrow(() -> new InvalidCredentialsException());

        // Validar senha com BCrypt
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // Q2.7: gating configurável. Só bloqueia contas EMAIL não-verificadas quando
        // o enforcement estrito está ligado — usuários OAuth (Google) já entram
        // verificados e não são afetados.
        if (requireEmailVerification
                && user.getAuthProvider() == AuthProvider.EMAIL
                && !user.isEmailVerified()) {
            throw new EmailNotVerifiedException(
                "E-mail não verificado. Verifique sua caixa de entrada ou solicite um novo link de verificação.");
        }

        return user;
    }

    /**
     * Gera um username único a partir de um email
     * <p>
     * Extrai o prefixo do email (parte antes do @), normaliza,
     * e adiciona número incremental se já existir.
     *
     * @param email email para gerar username
     * @return username único disponível
     */
    @Transactional(readOnly = true)
    public String generateUniqueUsername(String email) {
        // Extrair prefixo do email (antes do @)
        String baseUsername = email.split("@")[0];

        // Remover caracteres especiais e normalizar
        baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        // Limitar tamanho máximo
        if (baseUsername.length() > 20) {
            baseUsername = baseUsername.substring(0, 20);
        }

        // Se ficou vazio após normalização, usar fallback
        if (baseUsername.isBlank()) {
            baseUsername = "user";
        }

        // Verificar disponibilidade e adicionar contador se necessário
        String username = baseUsername;
        int counter = 1;

        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}
