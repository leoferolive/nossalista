package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REGRESSÃO do login Google (hotfix): o início do fluxo OAuth2 deve guardar o
 * authorization-request (state anti-CSRF) num COOKIE, e NÃO na HttpSession.
 *
 * <p>Este teste EXIGE a mudança de comportamento: com o repositório padrão
 * (baseado em HttpSession, usado sob STATELESS) NÃO existe o cookie
 * {@code OAUTH2_AUTH_REQUEST} — o state ficava só na sessão (que o STATELESS não
 * mantém), quebrando a validação no callback e emitindo one-time codes órfãos.
 * Com o {@link CookieOAuth2AuthorizationRequestRepository} registrado no
 * authorizationEndpoint, o redirect para o Google passa a setar esse cookie.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DisplayName("OAuth2 authorization-request persistido em cookie (não em sessão)")
class OAuth2AuthorizationRequestCookieIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /oauth2/authorization/google redireciona ao Google E grava o state no cookie OAUTH2_AUTH_REQUEST")
    void authorizationEndpointStoresStateInCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Redireciona para o consent do Google, com o state do fluxo.
        String location = result.getResponse().getRedirectedUrl();
        assertThat(location).contains("accounts.google.com").contains("state=");

        // O authorization-request é persistido no NOSSO cookie (não na sessão).
        List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies)
                .as("o redirect de autorização deve setar o cookie do authorization-request")
                .anyMatch(c -> c.contains(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME + "="));

        // E o cookie é endurecido (HttpOnly + SameSite=Lax) — não vaza via JS e
        // sobrevive ao retorno top-level do Google.
        String authCookie = setCookies.stream()
                .filter(c -> c.contains(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME + "="))
                .findFirst().orElseThrow();
        assertThat(authCookie).contains("HttpOnly").contains("SameSite=Lax");
    }
}
