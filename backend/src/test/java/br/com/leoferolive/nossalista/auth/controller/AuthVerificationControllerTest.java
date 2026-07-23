package br.com.leoferolive.nossalista.auth.controller;

import br.com.leoferolive.nossalista.auth.domain.EmailVerificationToken;
import br.com.leoferolive.nossalista.auth.repository.EmailVerificationTokenRepository;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.auth.service.OAuthCodeStore;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para os endpoints de OAuth exchange (Q2.3) e verificação
 * de e-mail (Q2.7) do {@link AuthController}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@RegressionTest
class AuthVerificationControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OAuthCodeStore oauthCodeStore;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    private User persistUser(AuthProvider provider, boolean verified) {
        User user = new User();
        user.setEmail("verify-" + UUID.randomUUID() + "@example.com");
        user.setUsername("u" + UUID.randomUUID().toString().substring(0, 8));
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setAuthProvider(provider);
        user.setRole(Role.USER);
        user.setEmailVerified(verified);
        return userRepository.save(user);
    }

    // ----- Q2.3: OAuth one-time code exchange -----

    @Test
    void exchangeWithValidCodeCreatesSessionCookie() throws Exception {
        User user = persistUser(AuthProvider.GOOGLE, true);
        String jwt = jwtService.generateToken(user);
        String code = oauthCodeStore.issue(jwt);

        Map<String, String> request = new HashMap<>();
        request.put("code", code);

        MvcResult exchange = mockMvc.perform(post("/api/auth/oauth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.email").value(user.getEmail()))
            .andReturn();

        String setCookie = exchange.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("nl_session=" + jwt).contains("HttpOnly");
    }

    @Test
    void exchangeWithInvalidCodeReturns400() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("code", "nao-existe");

        mockMvc.perform(post("/api/auth/oauth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/invalid-oauth-code"))
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void exchangeIsSingleUse() throws Exception {
        User user = persistUser(AuthProvider.GOOGLE, true);
        String jwt = jwtService.generateToken(user);
        String code = oauthCodeStore.issue(jwt);

        Map<String, String> request = new HashMap<>();
        request.put("code", code);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/oauth/exchange")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());

        // Segunda troca do mesmo code → 400 (single-use)
        mockMvc.perform(post("/api/auth/oauth/exchange")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    // ----- Q2.7: email verification -----

    @Test
    void verifyEmailWithValidTokenMarksVerifiedAndConsumesToken() throws Exception {
        User user = persistUser(AuthProvider.EMAIL, false);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setToken("integration-valid-token");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        tokenRepository.save(token);

        mockMvc.perform(get("/api/auth/verify-email").param("token", "integration-valid-token"))
            .andExpect(status().isOk());

        User refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refreshed.isEmailVerified()).isTrue();
        assertThat(tokenRepository.findByTokenAndUsedFalse("integration-valid-token")).isEmpty();
    }

    @Test
    void verifyEmailWithInvalidTokenReturns400() throws Exception {
        mockMvc.perform(get("/api/auth/verify-email").param("token", "bogus-token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/invalid-verification-token"))
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void resendVerificationAlwaysReturns200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "nobody-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }
}
