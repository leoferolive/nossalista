package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.AuthProvider;
import br.com.leoferolive.nossalista.auth.domain.Role;
import br.com.leoferolive.nossalista.auth.domain.User;
import br.com.leoferolive.nossalista.auth.dto.RegisterRequest;
import br.com.leoferolive.nossalista.auth.exception.EmailAlreadyExistsException;
import br.com.leoferolive.nossalista.auth.exception.UsernameAlreadyExistsException;
import br.com.leoferolive.nossalista.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para operações de autenticação
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra um novo usuário com email e senha
     *
     * @param request dados de registro
     * @return usuário criado
     * @throws EmailAlreadyExistsException se email já existe
     * @throws UsernameAlreadyExistsException se username já existe
     */
    @Transactional
    public User register(RegisterRequest request) {
        // Normalize and trim input
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedUsername = request.username().trim().toLowerCase();
        String trimmedPassword = request.password().trim();
        String trimmedName = request.name() != null ? request.name().trim() : null;

        // Check for duplicate email
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        // Check for duplicate username
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new UsernameAlreadyExistsException(normalizedUsername);
        }

        // Create user with hashed password
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(trimmedPassword)); // Hash password with BCrypt
        user.setName(trimmedName);
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(Role.USER); // Set default role

        return userRepository.save(user);
    }
}
