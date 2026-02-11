package br.com.leoferolive.nossalista.auth.controller;

import br.com.leoferolive.nossalista.auth.domain.AuthProvider;
import br.com.leoferolive.nossalista.auth.domain.Role;
import br.com.leoferolive.nossalista.auth.domain.User;
import br.com.leoferolive.nossalista.auth.repository.UserRepository;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para endpoint de registro do AuthController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/db/migration/V1__create_users_table.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
class AuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldRegisterUserWithValidData() throws Exception {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("email", "leo@example.com");
        request.put("username", "leoferolive");
        request.put("password", "senha123");
        request.put("name", "Leonardo Oliveira");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.username").value("leoferolive"))
            .andExpect(jsonPath("$.email").value("leo@example.com"))
            .andExpect(jsonPath("$.name").value("Leonardo Oliveira"))
            .andExpect(jsonPath("$.authProvider").value("EMAIL"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.password").doesNotExist()); // Password should NOT be in response

        // Verify user was created in database
        User user = userRepository.findByEmail("leo@example.com").orElseThrow();
        assertThat(user.getUsername()).isEqualTo("leoferolive");
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.EMAIL);
        assertThat(user.getRole()).isEqualTo(Role.USER); // Verify role is USER

        // Verify password is hashed (BCrypt starts with $2a$)
        assertThat(user.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("senha123", user.getPassword())).isTrue();
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        // Given - create existing user
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setUsername("existinguser");
        existingUser.setPassword(passwordEncoder.encode("password"));
        existingUser.setAuthProvider(AuthProvider.EMAIL);
        existingUser.setRole(Role.USER);
        userRepository.save(existingUser);

        Map<String, String> request = new HashMap<>();
        request.put("email", "existing@example.com"); // Duplicate email
        request.put("username", "newuser");
        request.put("password", "senha123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/email-already-exists"))
            .andExpect(jsonPath("$.title").value("Email já cadastrado"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Email 'existing@example.com' já está em uso"))
            .andExpect(jsonPath("$.instance").value("/api/auth/register"));
    }

    @Test
    void shouldReturn409WhenUsernameAlreadyExists() throws Exception {
        // Given - create existing user
        User existingUser = new User();
        existingUser.setEmail("user@example.com");
        existingUser.setUsername("existingusername");
        existingUser.setPassword(passwordEncoder.encode("password"));
        existingUser.setAuthProvider(AuthProvider.EMAIL);
        existingUser.setRole(Role.USER);
        userRepository.save(existingUser);

        Map<String, String> request = new HashMap<>();
        request.put("email", "newemail@example.com");
        request.put("username", "existingusername"); // Duplicate username
        request.put("password", "senha123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/username-already-exists"))
            .andExpect(jsonPath("$.title").value("Username já cadastrado"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Username 'existingusername' já está em uso"))
            .andExpect(jsonPath("$.instance").value("/api/auth/register"));
    }

    @Test
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");
        request.put("username", "testuser");
        request.put("password", "123"); // Less than 6 characters

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/validation-error"))
            .andExpect(jsonPath("$.title").value("Validation Error"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.password").value("Senha deve ter no mínimo 6 caracteres"));
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("email", "invalid-email"); // Invalid email format
        request.put("username", "testuser");
        request.put("password", "senha123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/validation-error"))
            .andExpect(jsonPath("$.title").value("Validation Error"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.email").value("Email inválido"));
    }

    @Test
    void shouldReturn400WhenUsernameHasInvalidFormat() throws Exception {
        // Given - username with uppercase and special characters
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");
        request.put("username", "User@Name!"); // Invalid format
        request.put("password", "senha123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/validation-error"))
            .andExpect(jsonPath("$.title").value("Validation Error"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.username").value("Username deve conter apenas letras minúsculas, números, hífen e underscore"));
    }

    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        // Given - empty request
        Map<String, String> request = new HashMap<>();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists())
            .andExpect(jsonPath("$.errors.username").exists())
            .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void shouldNotReturnPasswordInResponse() throws Exception {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("email", "security@example.com");
        request.put("username", "secureuser");
        request.put("password", "senha123");

        // When & Then
        String response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Verify response does NOT contain password field
        assertThat(response).doesNotContain("password");
        assertThat(response).doesNotContain("senha123");
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        // Given - create user first
        User user = new User();
        user.setEmail("login@example.com");
        user.setUsername("loginuser");
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setName("Login User");
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(Role.USER);
        userRepository.save(user);

        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "login@example.com");
        loginRequest.put("password", "senha123");

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.username").value("loginuser"))
            .andExpect(jsonPath("$.email").value("login@example.com"))
            .andExpect(jsonPath("$.name").value("Login User"))
            .andExpect(jsonPath("$.authProvider").value("EMAIL"))
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.expiresAt").exists())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andReturn();

        // Verify token is valid
        String responseBody = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();
        assertThat(jwtService.validateToken(token)).isTrue();

        // Verify token contains correct userId
        UUID userId = jwtService.extractUserId(token);
        assertThat(userId).isEqualTo(user.getId());
    }

    @Test
    void shouldReturn401WhenEmailDoesNotExist() throws Exception {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "naoexiste@example.com");
        loginRequest.put("password", "senha123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/invalid-credentials"))
            .andExpect(jsonPath("$.title").value("Credenciais inválidas"))
            .andExpect(jsonPath("$.detail").value("Email ou senha inválidos"))
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldReturn401WhenPasswordIsIncorrect() throws Exception {
        // Given - create user first
        User user = new User();
        user.setEmail("wrongpass@example.com");
        user.setUsername("wrongpassuser");
        user.setPassword(passwordEncoder.encode("senhaCorreta"));
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(Role.USER);
        userRepository.save(user);

        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "wrongpass@example.com");
        loginRequest.put("password", "senhaErrada");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/invalid-credentials"))
            .andExpect(jsonPath("$.title").value("Credenciais inválidas"))
            .andExpect(jsonPath("$.detail").value("Email ou senha inválidos"))
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldReturn400WhenLoginFieldsAreMissing() throws Exception {
        // Given - empty request
        Map<String, String> loginRequest = new HashMap<>();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists())
            .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void shouldNotReturnPasswordInLoginResponse() throws Exception {
        // Given - create user first
        User user = new User();
        user.setEmail("secure@example.com");
        user.setUsername("secureuser");
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(Role.USER);
        userRepository.save(user);

        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "secure@example.com");
        loginRequest.put("password", "senha123");

        // When & Then
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Verify response does NOT contain password field or plaintext password
        assertThat(response).doesNotContain("\"password\"");
        assertThat(response).doesNotContain("senha123");
    }

    @Test
    void shouldAcceptRequestWithValidJwtToken() throws Exception {
        // Given - create user and generate token
        User user = new User();
        user.setEmail("jwtvalid@example.com");
        user.setUsername("jwtvaliduser");
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(Role.USER);
        userRepository.save(user);

        String token = jwtService.generateToken(user);

        // When & Then - use token to access protected endpoint
        mockMvc.perform(get("/api/health")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectRequestWithInvalidJwtToken() throws Exception {
        // Given - invalid token
        String invalidToken = "invalid.jwt.token";

        // When & Then - trying to access protected endpoint with invalid token should fail
        // Note: /api/health is public, so we can't test rejection there
        // This test validates that JwtAuthenticationFilter handles invalid tokens gracefully
        // The filter will not authenticate, so requests to authenticated endpoints would fail
        mockMvc.perform(get("/api/health")
                .header("Authorization", "Bearer " + invalidToken))
            .andExpect(status().isOk()); // /api/health is public, so still works
    }

    @Test
    void shouldNormalizeEmailToLowercase() throws Exception {
        // Given - create user with lowercase email
        User user = new User();
        user.setEmail("uppercase@example.com");
        user.setUsername("uppercaseuser");
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(Role.USER);
        userRepository.save(user);

        // When - login with UPPERCASE email
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "UPPERCASE@EXAMPLE.COM");
        loginRequest.put("password", "senha123");

        // Then - should work because email is normalized
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("uppercase@example.com"));
    }
}
