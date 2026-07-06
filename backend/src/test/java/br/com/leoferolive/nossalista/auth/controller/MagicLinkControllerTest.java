package br.com.leoferolive.nossalista.auth.controller;

import br.com.leoferolive.nossalista.auth.domain.MagicLinkToken;
import br.com.leoferolive.nossalista.auth.repository.MagicLinkTokenRepository;
import br.com.leoferolive.nossalista.config.RateLimiterService;
import br.com.leoferolive.nossalista.support.RegressionTest;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@RegressionTest
class MagicLinkControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private UserRepository userRepository;
    @Autowired private MagicLinkTokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RateLimiterService rateLimiterService;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        rateLimiterService.reset();
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setUsername("u" + UUID.randomUUID().toString().substring(0, 8));
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(Role.USER);
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    @Test
    void requestMagicLinkAlwaysReturns200ForUnknownEmail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "nobody-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void requestMagicLinkCreatesTokenForExistingUser() throws Exception {
        User user = persistUser("ana-" + UUID.randomUUID() + "@example.com");
        Map<String, String> request = new HashMap<>();
        request.put("email", user.getEmail());

        mockMvc.perform(post("/api/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        assertThat(tokenRepository.findAll())
            .anyMatch(t -> t.getUserId().equals(user.getId()) && !t.isUsed());
    }

    @Test
    void magicLoginWithValidTokenReturnsJwtAndVerifiesEmail() throws Exception {
        User user = persistUser("login-" + UUID.randomUUID() + "@example.com");
        MagicLinkToken token = new MagicLinkToken();
        token.setUserId(user.getId());
        token.setToken("valid-magic-token");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(token);

        Map<String, String> request = new HashMap<>();
        request.put("token", "valid-magic-token");

        mockMvc.perform(post("/api/auth/magic-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.email").value(user.getEmail()));

        User refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refreshed.isEmailVerified()).isTrue();
        assertThat(tokenRepository.findByTokenAndUsedFalse("valid-magic-token")).isEmpty();
    }

    @Test
    void magicLoginWithInvalidTokenReturns400() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("token", "bogus");

        mockMvc.perform(post("/api/auth/magic-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/invalid-magic-link-token"))
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void requestMagicLinkReturns429AfterEmailRateLimit() throws Exception {
        User user = persistUser("rl-" + UUID.randomUUID() + "@example.com");
        Map<String, String> request = new HashMap<>();
        request.put("email", user.getEmail());
        String body = objectMapper.writeValueAsString(request);

        // Limite por e-mail = 5/1h: as 5 primeiras passam, a 6ª estoura.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/magic-link")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void magicLoginReturns429AfterIpRateLimit() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("token", "bogus-" + UUID.randomUUID());
        String body = objectMapper.writeValueAsString(request);

        // Limite por IP = 10/15min: as 10 primeiras respondem 400 (token inválido),
        // a 11ª estoura o rate limit (o check ocorre antes do consume).
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/magic-login")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/auth/magic-login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isTooManyRequests());
    }
}
