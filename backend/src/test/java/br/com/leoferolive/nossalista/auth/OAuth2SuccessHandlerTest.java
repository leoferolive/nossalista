package br.com.leoferolive.nossalista.auth;

import br.com.leoferolive.nossalista.auth.service.AuthService;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes para OAuth2SuccessHandler
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2SuccessHandler")
class OAuth2SuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private OAuth2SuccessHandler successHandler;

    private static final String FRONTEND_URL = "http://localhost:5173";
    private static final String TEST_EMAIL = "leo@gmail.com";
    private static final String TEST_NAME = "Leonardo Oliveira";
    private static final String TEST_PICTURE = "https://lh3.googleusercontent.com/a/test";
    private static final String TEST_USERNAME = "leo";
    private static final String TEST_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2SuccessHandler(userRepository, jwtService, authService);
        successHandler.setFrontendUrl(FRONTEND_URL);
    }

    @Test
    @DisplayName("Deve criar novo usuário quando email não existe")
    void shouldCreateNewUserWhenEmailNotExists() throws IOException {
        // Given: OAuth2 token com dados do Google
        OAuth2AuthenticationToken oauth2Token = createOAuth2Token(TEST_EMAIL, TEST_NAME, TEST_PICTURE);

        // Given: Email não existe no database
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Given: Username único gerado
        when(authService.generateUniqueUsername(TEST_EMAIL)).thenReturn(TEST_USERNAME);

        // Given: Novo usuário salvo
        User newUser = createUser(UUID.randomUUID(), TEST_USERNAME, TEST_EMAIL, TEST_NAME, TEST_PICTURE, AuthProvider.GOOGLE, null);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Given: JWT gerado
        when(jwtService.generateToken(any(User.class))).thenReturn(TEST_JWT);

        // When: Handler processa autenticação
        successHandler.onAuthenticationSuccess(request, response, oauth2Token);

        // Then: Verifica que novo usuário foi criado
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(savedUser.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(savedUser.getName()).isEqualTo(TEST_NAME);
        assertThat(savedUser.getAvatarUrl()).isEqualTo(TEST_PICTURE);
        assertThat(savedUser.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(savedUser.getPassword()).isNull();

        // Then: Verifica que JWT foi gerado
        verify(jwtService).generateToken(any(User.class));

        // Then: Verifica redirect para frontend com token
        verify(response).sendRedirect(FRONTEND_URL + "/auth/callback?token=" + TEST_JWT);
    }

    @Test
    @DisplayName("Deve atualizar usuário existente quando dados mudaram")
    void shouldUpdateExistingUserWhenDataChanged() throws IOException {
        // Given: Usuário existente no database
        User existingUser = createUser(
            UUID.randomUUID(),
            TEST_USERNAME,
            TEST_EMAIL,
            "Old Name",
            "https://old-picture.com",
            AuthProvider.GOOGLE,
            null
        );
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

        // Given: OAuth2 token com dados atualizados
        OAuth2AuthenticationToken oauth2Token = createOAuth2Token(TEST_EMAIL, TEST_NAME, TEST_PICTURE);

        // Given: JWT gerado
        when(jwtService.generateToken(any(User.class))).thenReturn(TEST_JWT);

        // When: Handler processa autenticação
        successHandler.onAuthenticationSuccess(request, response, oauth2Token);

        // Then: Verifica que usuário foi atualizado
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User updatedUser = userCaptor.getValue();
        assertThat(updatedUser.getName()).isEqualTo(TEST_NAME);
        assertThat(updatedUser.getAvatarUrl()).isEqualTo(TEST_PICTURE);

        // Then: Verifica redirect com token
        verify(response).sendRedirect(FRONTEND_URL + "/auth/callback?token=" + TEST_JWT);
    }

    @Test
    @DisplayName("Não deve atualizar usuário quando dados não mudaram")
    void shouldNotUpdateUserWhenDataNotChanged() throws IOException {
        // Given: Usuário existente com mesmos dados
        User existingUser = createUser(
            UUID.randomUUID(),
            TEST_USERNAME,
            TEST_EMAIL,
            TEST_NAME,
            TEST_PICTURE,
            AuthProvider.GOOGLE,
            null
        );
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

        // Given: OAuth2 token com mesmos dados
        OAuth2AuthenticationToken oauth2Token = createOAuth2Token(TEST_EMAIL, TEST_NAME, TEST_PICTURE);

        // Given: JWT gerado
        when(jwtService.generateToken(any(User.class))).thenReturn(TEST_JWT);

        // When: Handler processa autenticação
        successHandler.onAuthenticationSuccess(request, response, oauth2Token);

        // Then: Não deve chamar save (dados iguais)
        verify(userRepository, never()).save(any(User.class));

        // Then: Deve gerar token e redirecionar
        verify(jwtService).generateToken(existingUser);
        verify(response).sendRedirect(FRONTEND_URL + "/auth/callback?token=" + TEST_JWT);
    }

    @Test
    @DisplayName("Deve gerar username único usando AuthService")
    void shouldGenerateUniqueUsernameViaAuthService() throws IOException {
        // Given: OAuth2 token
        OAuth2AuthenticationToken oauth2Token = createOAuth2Token(TEST_EMAIL, TEST_NAME, TEST_PICTURE);

        // Given: Email não existe
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Given: AuthService gera username único
        when(authService.generateUniqueUsername(TEST_EMAIL)).thenReturn("leo1");

        // Given: User salvo
        User newUser = createUser(UUID.randomUUID(), "leo1", TEST_EMAIL, TEST_NAME, TEST_PICTURE, AuthProvider.GOOGLE, null);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Given: JWT gerado
        when(jwtService.generateToken(any(User.class))).thenReturn(TEST_JWT);

        // When: Handler processa
        successHandler.onAuthenticationSuccess(request, response, oauth2Token);

        // Then: Deve chamar generateUniqueUsername
        verify(authService).generateUniqueUsername(TEST_EMAIL);

        // Then: Username deve ser "leo1"
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("leo1");
    }

    @Test
    @DisplayName("Deve normalizar email (trim + toLowerCase)")
    void shouldNormalizeEmail() throws IOException {
        // Given: OAuth2 token com email maiúsculo e espaços
        OAuth2AuthenticationToken oauth2Token = createOAuth2Token("  Leo@Gmail.COM  ", TEST_NAME, TEST_PICTURE);

        // Given: Email normalizado não existe
        when(userRepository.findByEmail("leo@gmail.com")).thenReturn(Optional.empty());

        // Given: Username gerado
        when(authService.generateUniqueUsername("leo@gmail.com")).thenReturn(TEST_USERNAME);

        // Given: User salvo
        User newUser = createUser(UUID.randomUUID(), TEST_USERNAME, "leo@gmail.com", TEST_NAME, TEST_PICTURE, AuthProvider.GOOGLE, null);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Given: JWT gerado
        when(jwtService.generateToken(any(User.class))).thenReturn(TEST_JWT);

        // When: Handler processa
        successHandler.onAuthenticationSuccess(request, response, oauth2Token);

        // Then: Email deve ser normalizado
        verify(userRepository).findByEmail("leo@gmail.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("leo@gmail.com");
    }

    @Test
    @DisplayName("Deve lançar exception se email for null (ausente no mapa)")
    void shouldThrowExceptionWhenEmailIsNull() {
        // Given: OAuth2 token SEM chave "email" no mapa de attributes
        OAuth2AuthenticationToken oauth2TokenNoEmail = createOAuth2TokenWithMissingEmail(TEST_NAME, TEST_PICTURE);

        // When/Then: Deve lançar IllegalStateException
        assertThatThrownBy(() -> successHandler.onAuthenticationSuccess(request, response, oauth2TokenNoEmail))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Email não fornecido pelo Google");
    }

    @Test
    @DisplayName("Deve lançar exception se email for vazio ou apenas espaços")
    void shouldThrowExceptionWhenEmailIsBlank() {
        // Given: OAuth2 token com email vazio
        OAuth2AuthenticationToken oauth2TokenEmptyEmail = createOAuth2Token("   ", TEST_NAME, TEST_PICTURE);

        // When/Then: Deve lançar IllegalStateException
        assertThatThrownBy(() -> successHandler.onAuthenticationSuccess(request, response, oauth2TokenEmptyEmail))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Email não fornecido pelo Google");
    }

    @Test
    @DisplayName("Não deve atualizar usuário EMAIL provider (migração não permitida)")
    void shouldNotUpdateEmailProviderUser() throws IOException {
        // Given: Usuário existente com EMAIL provider
        User existingUser = createUser(
            UUID.randomUUID(),
            TEST_USERNAME,
            TEST_EMAIL,
            "Old Name",
            null,
            AuthProvider.EMAIL,
            "hashed-password"
        );
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

        // Given: OAuth2 token
        OAuth2AuthenticationToken oauth2Token = createOAuth2Token(TEST_EMAIL, TEST_NAME, TEST_PICTURE);

        // Given: JWT gerado
        when(jwtService.generateToken(any(User.class))).thenReturn(TEST_JWT);

        // When: Handler processa
        successHandler.onAuthenticationSuccess(request, response, oauth2Token);

        // Then: Não deve atualizar (authProvider é EMAIL, não GOOGLE)
        verify(userRepository, never()).save(any(User.class));

        // Then: Deve gerar token normalmente
        verify(jwtService).generateToken(existingUser);
        verify(response).sendRedirect(FRONTEND_URL + "/auth/callback?token=" + TEST_JWT);
    }

    // Helper methods

    private OAuth2AuthenticationToken createOAuth2Token(String email, String name, String picture) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-user-id-123");
        attributes.put("email", email != null ? email : "");
        attributes.put("name", name != null ? name : "");
        attributes.put("picture", picture != null ? picture : "");

        OAuth2User oauth2User = new DefaultOAuth2User(
            null,
            attributes,
            "sub"
        );

        return new OAuth2AuthenticationToken(oauth2User, null, "google");
    }

    /**
     * Cria OAuth2 token SEM a chave "email" no mapa de attributes.
     * Simula cenário real onde Google não retorna email.
     */
    private OAuth2AuthenticationToken createOAuth2TokenWithMissingEmail(String name, String picture) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-user-id-123");
        // "email" key intentionally absent
        attributes.put("name", name != null ? name : "");
        attributes.put("picture", picture != null ? picture : "");

        OAuth2User oauth2User = new DefaultOAuth2User(
            null,
            attributes,
            "sub"
        );

        return new OAuth2AuthenticationToken(oauth2User, null, "google");
    }

    private User createUser(UUID id, String username, String email, String name, String avatarUrl, AuthProvider authProvider, String password) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setName(name);
        user.setAvatarUrl(avatarUrl);
        user.setAuthProvider(authProvider);
        user.setPassword(password);
        user.setRole(Role.USER);
        return user;
    }
}
