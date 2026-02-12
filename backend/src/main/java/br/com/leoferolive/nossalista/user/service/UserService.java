package br.com.leoferolive.nossalista.user.service;

import br.com.leoferolive.nossalista.auth.exception.EmailAlreadyExistsException;
import br.com.leoferolive.nossalista.auth.exception.UsernameAlreadyExistsException;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para operações CRUD de usuários
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Busca um usuário por email
     *
     * @param email email do usuário
     * @return usuário encontrado
     * @throws RuntimeException se usuário não existe
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + email));
    }

    /**
     * Busca um usuário por email (opcional)
     *
     * @param email email do usuário
     * @return Optional contendo o usuário se encontrado
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmailOptional(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Busca um usuário por username
     *
     * @param username username do usuário
     * @return usuário encontrado
     * @throws RuntimeException se usuário não existe
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + username));
    }

    /**
     * Busca um usuário por ID
     *
     * @param id ID do usuário
     * @return Optional contendo o usuário se encontrado
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Valida se um email já existe no sistema
     *
     * @param email email para validar
     * @throws EmailAlreadyExistsException se email já existe
     */
    public void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    /**
     * Valida se um username já existe no sistema
     *
     * @param username username para validar
     * @throws UsernameAlreadyExistsException se username já existe
     */
    public void validateUsernameNotExists(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }
    }

    /**
     * Cria um novo usuário no sistema
     *
     * @param username         username do usuário
     * @param email            email do usuário
     * @param hashedPassword   senha já criptografada
     * @param name             nome completo do usuário
     * @param authProvider     provedor de autenticação
     * @return usuário criado
     */
    @Transactional
    public User createUser(String username, String email, String hashedPassword,
                          String name, AuthProvider authProvider) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(hashedPassword);
        user.setName(name);
        user.setAuthProvider(authProvider);
        user.setRole(Role.USER); // Default role

        return userRepository.save(user);
    }

    /**
     * Salva ou atualiza um usuário
     *
     * @param user usuário a ser salvo
     * @return usuário salvo
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
}
