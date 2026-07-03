package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que os 3 pontos que escrevem {@code ProblemDetail} manualmente via
 * {@code response.getWriter()} ({@link Http401UnauthorizedEntryPoint},
 * {@link Http403AccessDeniedHandler} e o 429 de
 * {@link PersonalAccessTokenAuthenticationFilter}) declaram charset UTF-8
 * explicitamente — sem isso o Tomcat cai em ISO-8859-1 por padrão e mensagens
 * acentuadas ("Não autenticado", "Você não tem permissão", "tentativas de
 * autenticação") chegam corrompidas ao cliente. Cada teste inspeciona os
 * bytes crus da resposta para provar que representam UTF-8 (e não a
 * codificação incorreta de fallback), não apenas o texto já decodificado
 * pelo {@code MockHttpServletResponse} (que usa o charset declarado, então um
 * charset errado mascararia o bug em {@code getContentAsString()} sem
 * argumento).
 *
 * <p>O caso 403 precisa disparar {@link Http403AccessDeniedHandler}
 * especificamente — não {@code GlobalExceptionHandler.handleAccessDenied},
 * que trata {@code AccessDeniedException} lançada por {@code @PreAuthorize}
 * (method security, dentro do dispatch do MVC) e teria mascarado o teste.
 * {@code Http403AccessDeniedHandler} só é o {@code accessDeniedHandler} do
 * {@code exceptionHandling()} do Spring Security, acionado quando a negação
 * acontece no filtro de autorização (nível de URL), ANTES do dispatch — o
 * caso real disso em produção é {@code apiAccessManager()} bloqueando um PAT
 * de escopo {@code READ} tentando um método HTTP não-seguro em {@code
 * /api/**}.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("Charset UTF-8 nas respostas de erro manuais (401/403/429)")
class ErrorResponseCharsetIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private UserService userService;

    @Autowired
    private PersonalAccessTokenService tokenService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        rateLimiterService.reset();
    }

    @AfterEach
    void tearDown() {
        rateLimiterService.reset();
    }

    @Test
    @DisplayName("401 (Http401UnauthorizedEntryPoint) declara charset UTF-8 e preserva acentos")
    void unauthorizedResponseUsesUtf8Charset() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/lists"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertUtf8ResponseContains(result, "Autenticação necessária para acessar este recurso");
    }

    @Test
    @DisplayName("403 (Http403AccessDeniedHandler) declara charset UTF-8 e preserva acentos")
    void accessDeniedResponseUsesUtf8Charset() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userService.createUser(
            "charsetuser" + suffix, "charsetuser" + suffix + "@example.com", "hashed",
            "Charset User " + suffix, AuthProvider.EMAIL);
        String readOnlyToken = tokenService.create(
            user.getId(), new CreatePersonalAccessTokenRequest("charset-test-token", TokenScope.READ, null)
        ).token();

        // Um PAT de escopo READ tentando um método HTTP não-seguro (POST) em
        // /api/** é bloqueado por apiAccessManager() no filtro de autorização,
        // antes de chegar a qualquer controller — exercita Http403AccessDeniedHandler.
        MvcResult result = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + readOnlyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mercado\",\"typeId\":1}"))
                .andExpect(status().isForbidden())
                .andReturn();

        assertUtf8ResponseContains(result, "Você não tem permissão para acessar este recurso");
    }

    @Test
    @DisplayName("429 (PersonalAccessTokenAuthenticationFilter) declara charset UTF-8 e preserva acentos")
    void tooManyRequestsResponseUsesUtf8Charset() throws Exception {
        String invalidPatHeader = "Bearer nlmcp_" + "0".repeat(64);

        // Esgota o limite de tentativas inválidas (INVALID_ATTEMPTS_LIMIT) para
        // este IP; a próxima requisição já cai no bloqueio 429.
        for (int i = 0; i < PersonalAccessTokenAuthenticationFilter.INVALID_ATTEMPTS_LIMIT; i++) {
            mockMvc.perform(get("/api/health").header("Authorization", invalidPatHeader));
        }

        MvcResult result = mockMvc.perform(get("/api/health").header("Authorization", invalidPatHeader))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        assertUtf8ResponseContains(
            result, "Muitas tentativas de autenticação com token inválido. Tente novamente mais tarde.");
    }

    private void assertUtf8ResponseContains(MvcResult result, String expectedText) throws Exception {
        var response = result.getResponse();
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        assertThat(response.getContentType()).containsIgnoringCase("charset=utf-8");

        // Decodifica os bytes crus como UTF-8 explicitamente: se a implementação
        // regredisse (voltasse a depender do fallback ISO-8859-1 do Tomcat), os
        // bytes multi-byte dos acentos teriam sido escritos errado e esta
        // decodificação continuaria mostrando o dado corrompido.
        String decoded = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(decoded).contains(expectedText);
    }
}
