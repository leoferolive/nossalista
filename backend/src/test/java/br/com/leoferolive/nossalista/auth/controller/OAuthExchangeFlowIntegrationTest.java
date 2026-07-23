package br.com.leoferolive.nossalista.auth.controller;

import br.com.leoferolive.nossalista.auth.OAuth2SuccessHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static br.com.leoferolive.nossalista.support.SessionCookieRequestPostProcessor.session;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REGRESSÃO do login Google (Q2.3): cobre o fluxo de ponta a ponta que quebrava
 * em produção — emitir o one-time code no {@link OAuth2SuccessHandler} (agora
 * PERSISTIDO no banco), trocar via {@code POST /api/auth/oauth/exchange} e usar o
 * cookie de sessão retornado num endpoint autenticado ({@code GET /api/lists}).
 *
 * <p>Antes, o code vivia em memória por instância; o exchange respondia 400
 * quando emitido numa instância e trocado em outra (ou após restart), deixando o
 * usuário sem sessão (401 em tudo). Este teste trava o contrato persistido.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class OAuthExchangeFlowIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OAuth2SuccessHandler oauth2SuccessHandler;

    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Login Google: code emite cookie de sessão que acessa /api/lists")
    void googleLoginCodeExchangesForUsableSessionCookie() throws Exception {
        // 1) Sucesso do OAuth2 (Google) emite o one-time code e redireciona para o frontend
        MockHttpServletResponse redirectResponse = new MockHttpServletResponse();
        oauth2SuccessHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                redirectResponse,
                googleToken("novo.google@example.com", "Novo Google", "https://lh3.googleusercontent.com/a/x"));

        String redirectedUrl = redirectResponse.getRedirectedUrl();
        assertThat(redirectedUrl).contains("/auth/callback?code=");
        // Segurança: o JWT nunca aparece na URL (apenas o code opaco)
        assertThat(redirectedUrl).doesNotContain("token=");
        String code = redirectedUrl.substring(redirectedUrl.indexOf("code=") + "code=".length());

        // 2) Frontend troca o code pela sessão HttpOnly
        MvcResult exchange = mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}")
                        .with(session("csrf-only")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.email").value("novo.google@example.com"))
                .andReturn();

        String setCookie = exchange.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("nl_session=").contains("HttpOnly").contains("SameSite=Lax");
        String jwt = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));

        // 3) O cookie trocado autentica de verdade um endpoint protegido
        mockMvc.perform(get("/api/lists").cookie(new Cookie("nl_session", jwt)))
                .andExpect(status().isOk());

        // 4) Single-use: o mesmo code não pode ser trocado de novo
        mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}")
                        .with(session("csrf-only")))
                .andExpect(status().isBadRequest());
    }

    private OAuth2AuthenticationToken googleToken(String email, String name, String picture) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-" + email);
        attributes.put("email", email);
        attributes.put("name", name);
        attributes.put("picture", picture);
        OAuth2User principal = new DefaultOAuth2User(null, attributes, "sub");
        return new OAuth2AuthenticationToken(principal, null, "google");
    }
}
