package br.com.leoferolive.nossalista.auth.controller;

import br.com.leoferolive.nossalista.auth.domain.AuthProvider;
import br.com.leoferolive.nossalista.auth.domain.User;
import br.com.leoferolive.nossalista.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController registration endpoint
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
}
