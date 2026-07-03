package br.com.leoferolive.nossalista.user.repository;

import br.com.leoferolive.nossalista.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Busca usuários por username (case-insensitive e parcial) com limite de 20 resultados
     *
     * @param username termo de busca
     * @return Lista de até 20 usuários que contêm o termo no username
     */
    List<User> findTop20ByUsernameContainingIgnoreCase(String username);

    /**
     * Busca um usuário travando a linha (lock pessimista de escrita) até o fim
     * da transação corrente. Usado para serializar operações que precisam
     * checar-e-agir de forma atômica por usuário (ex.: limite de Personal
     * Access Tokens ativos em {@code PersonalAccessTokenService.create}),
     * evitando TOCTOU quando duas requisições concorrentes do mesmo usuário
     * passam pela checagem de contagem antes de qualquer uma inserir.
     * Funciona de forma equivalente em H2 e PostgreSQL ({@code SELECT ... FOR UPDATE}).
     *
     * @param id ID do usuário
     * @return Optional contendo o usuário, com a linha travada até o commit/rollback
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);
}
