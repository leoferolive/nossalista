package br.com.leoferolive.nossalista.langflowembed.controller;

import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.support.SessionCookieRequestPostProcessor;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Embed do Langflow desligado (padrão): os endpoints respondem 404 e nenhuma chave é exigida.
 * O contexto sobe sem {@code LANGFLOW_EMBED_RSA_PRIVATE_KEY}, o que já prova que ela não é necessária.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class LangflowEmbedDisabledIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
        SecurityContextHolder.clearContext();
        user = userService.createUser(
            "ana.desligada", "ana.desligada@example.com", "hashed", "Ana Lista", AuthProvider.EMAIL);
    }

    @Test
    @DisplayName("launch com sessão responde 404 quando o embed está desligado")
    void launchWithSessionReturns404() throws Exception {
        mockMvc.perform(get("/api/langflow/launch")
                .with(SessionCookieRequestPostProcessor.session(jwtService.generateToken(user))))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("launch sem sessão também responde 404, sem revelar a rota com 401")
    void launchWithoutSessionReturns404() throws Exception {
        mockMvc.perform(get("/api/langflow/launch"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("JWKS responde 404 quando o embed está desligado")
    void jwksReturns404() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("refresh com sessão responde 404 quando o embed está desligado")
    void refreshWithSessionReturns404() throws Exception {
        mockMvc.perform(get("/api/langflow/refresh")
                .with(SessionCookieRequestPostProcessor.session(jwtService.generateToken(user))))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("refresh sem sessão também responde 404 quando o embed está desligado")
    void refreshWithoutSessionReturns404() throws Exception {
        mockMvc.perform(get("/api/langflow/refresh"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("logout com embed desligado expira a sessão e não grava nl_lf_token")
    void logoutDoesNotTouchLaunchCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                .with(SessionCookieRequestPostProcessor.session(jwtService.generateToken(user))))
            .andExpect(status().isNoContent())
            .andReturn();

        List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).anyMatch(header -> header.startsWith("nl_session="));
        assertThat(setCookies).noneMatch(header -> header.startsWith("nl_lf_token="));
    }
}
