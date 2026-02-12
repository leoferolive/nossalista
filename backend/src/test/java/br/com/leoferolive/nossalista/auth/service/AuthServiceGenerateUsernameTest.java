package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes para AuthService.generateUniqueUsername()
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - generateUniqueUsername")
class AuthServiceGenerateUsernameTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Deve gerar username a partir do prefixo do email")
    void shouldGenerateUsernameFromEmailPrefix() {
        // Given: Email simples
        String email = "leo@gmail.com";

        // Given: Username "leo" está disponível
        when(userRepository.findByUsername("leo")).thenReturn(Optional.empty());

        // When: Gera username
        String username = authService.generateUniqueUsername(email);

        // Then: Username deve ser "leo"
        assertThat(username).isEqualTo("leo");
    }

    @Test
    @DisplayName("Deve adicionar número incremental se username já existe")
    void shouldAddIncrementalNumberWhenUsernameExists() {
        // Given: Email
        String email = "leo@gmail.com";

        // Given: "leo" já existe, mas "leo1" está disponível
        User existingUser = new User();
        when(userRepository.findByUsername("leo")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("leo1")).thenReturn(Optional.empty());

        // When: Gera username
        String username = authService.generateUniqueUsername(email);

        // Then: Username deve ser "leo1"
        assertThat(username).isEqualTo("leo1");
    }

    @Test
    @DisplayName("Deve incrementar até encontrar username disponível")
    void shouldIncrementUntilFindingAvailableUsername() {
        // Given: Email
        String email = "leo@gmail.com";

        // Given: "leo", "leo1", "leo2" existem, mas "leo3" está disponível
        User existingUser = new User();
        when(userRepository.findByUsername("leo")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("leo1")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("leo2")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("leo3")).thenReturn(Optional.empty());

        // When: Gera username
        String username = authService.generateUniqueUsername(email);

        // Then: Username deve ser "leo3"
        assertThat(username).isEqualTo("leo3");
    }

    @Test
    @DisplayName("Deve remover caracteres especiais do email")
    void shouldRemoveSpecialCharactersFromEmail() {
        // Given: Email com caracteres especiais
        String email = "leonardo.oliveira+tag@gmail.com";

        // Given: Username normalizado está disponível
        when(userRepository.findByUsername("leonardooliveiratag")).thenReturn(Optional.empty());

        // When: Gera username
        String username = authService.generateUniqueUsername(email);

        // Then: Caracteres especiais devem ser removidos
        assertThat(username).isEqualTo("leonardooliveiratag");
    }

    @Test
    @DisplayName("Deve converter para lowercase")
    void shouldConvertToLowercase() {
        // Given: Email com maiúsculas
        String email = "Leonardo@Gmail.COM";

        // Given: Username em lowercase está disponível
        when(userRepository.findByUsername("leonardo")).thenReturn(Optional.empty());

        // When: Gera username
        String username = authService.generateUniqueUsername(email);

        // Then: Username deve estar em lowercase
        assertThat(username).isEqualTo("leonardo");
    }

    @Test
    @DisplayName("Deve limitar tamanho do username a 20 caracteres")
    void shouldLimitUsernameTo20Characters() {
        // Given: Email com prefixo muito longo
        String email = "verylongemailaddressthatexceedstwentycharacters@example.com";

        // Given: Username truncado está disponível
        when(userRepository.findByUsername("verylongemailaddress")).thenReturn(Optional.empty());

        // When: Gera username
        String username = authService.generateUniqueUsername(email);

        // Then: Username deve ter no máximo 20 caracteres
        assertThat(username).hasSize(20);
        assertThat(username).isEqualTo("verylongemailaddress");
    }

    @Test
    @DisplayName("Deve usar fallback 'user' se prefixo ficar vazio após normalização")
    void shouldUseFallbackWhenPrefixBecomesEmpty() {
        // Given: Email com apenas caracteres especiais no prefixo
        String email = "+++@example.com";

        // Given: "user" está disponível
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        // When: Gera username
        String username = authService.generateUniqueUsername(email);

        // Then: Username deve ser "user"
        assertThat(username).isEqualTo("user");
    }

    @Test
    @DisplayName("Deve adicionar número ao fallback se 'user' já existe")
    void shouldAddNumberToFallbackIfUserExists() {
        // Given: Email com apenas caracteres especiais
        String email = "@@@example.com";

        // Given: "user" já existe, mas "user1" está disponível
        User existingUser = new User();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());

        // When: Gera username
        String username = authService.generateUniqueUsername(email);

        // Then: Username deve ser "user1"
        assertThat(username).isEqualTo("user1");
    }
}
