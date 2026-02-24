package br.com.leoferolive.nossalista.user.repository;

import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações da entidade User
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Busca um usuário por endereço de email
     *
     * @param email o email para buscar
     * @return Optional contendo o usuário se encontrado
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca um usuário por username
     *
     * @param username o username para buscar
     * @return Optional contendo o usuário se encontrado
     */
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Verifica se um email já existe no banco de dados
     *
     * @param email o email para verificar
     * @return true se email existe, false caso contrário
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se um username já existe no banco de dados
     *
     * @param username o username para verificar
     * @return true se username existe, false caso contrário
     */
    boolean existsByUsername(String username);

    /**
     * Busca usuários por username (case-insensitive e parcial)
     *
     * @param query termo de busca
     * @return Lista de usuários que contêm o termo no username
     */
    List<User> findByUsernameContainingIgnoreCase(String query);
}
