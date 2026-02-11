package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.AuthProvider;
import br.com.leoferolive.nossalista.auth.domain.User;
import br.com.leoferolive.nossalista.auth.dto.RegisterRequest;
import br.com.leoferolive.nossalista.auth.exception.EmailAlreadyExistsException;
import br.com.leoferolive.nossalista.auth.exception.UsernameAlreadyExistsException;
import br.com.leoferolive.nossalista.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for authentication operations
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
     * Register a new user with email and password
     *
     * @param request registration data
     * @return created user
     * @throws EmailAlreadyExistsException if email already exists
     * @throws UsernameAlreadyExistsException if username already exists
     */
    @Transactional
    public User register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // Check for duplicate username
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        // Create user with hashed password
        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password())); // Hash password with BCrypt
        user.setName(request.name());
        user.setAuthProvider(AuthProvider.EMAIL);

        return userRepository.save(user);
    }
}
