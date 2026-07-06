package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.Callable;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Regressão de D-023 / alerta {@code NossalistaHighErrorRate}: uma requisição
 * autenticada que passa por um dispatch ASYNC (o transporte MCP Streamable HTTP
 * completa a resposta SSE via {@code AsyncContext.dispatch()}, redisparando o
 * {@code FilterChainProxy} inteiro) precisa continuar autorizada no segundo passo.
 *
 * <p>Antes do fix, os filtros de autenticação ({@code OncePerRequestFilter}) pulavam
 * o dispatch async por padrão ({@code shouldNotFilterAsyncDispatch() == true}); como
 * a app é STATELESS (identidade vem do header a cada request), o {@code SecurityContext}
 * ficava vazio no segundo passo e o {@code AuthorizationFilter} negava um token válido
 * com {@code AuthorizationDeniedException} sobre a resposta já commitada — virando 500
 * ({@code AnonymousAuthenticationToken}) e conexão cortada ({@code unexpected EOF} no
 * cloudflared). O fix faz os filtros re-autenticarem também no async.</p>
 *
 * <p>Reproduz o mecanismo com MockMvc + {@code asyncDispatch()} num endpoint async
 * protegido por {@code /api/**} (mesma regra {@code authenticated()} de {@code /mcp/**}),
 * de forma determinística — sem depender da concorrência real que faz o bug ser
 * intermitente em produção. O {@code SecurityContextHolderFilter} limpa o ThreadLocal
 * entre os dispatches, então o segundo passo só é autorizado se os filtros de auth
 * re-executarem.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:async-reauth;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
class AsyncDispatchReauthenticationTest {

    /**
     * Endpoint async mínimo sob {@code /api/**} (portanto {@code authenticated()}),
     * que sempre inicia processamento assíncrono via {@link Callable} — reproduzindo
     * o padrão do transporte MCP sem o SDK MCP.
     */
    @RestController
    static class AsyncProbeController {
        @GetMapping("/api/__async_probe")
        Callable<ResponseEntity<String>> probe() {
            return () -> ResponseEntity.ok("ok");
        }
    }

    @TestConfiguration
    static class ProbeConfig {
        @Bean
        AsyncProbeController asyncProbeController() {
            return new AsyncProbeController();
        }
    }

    @Autowired
    private org.springframework.web.context.WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private PersonalAccessTokenService tokenService;

    private MockMvc mockMvcWithSecurity() {
        MockMvcConfigurer security = springSecurity();
        return webAppContextSetup(webApplicationContext).apply(security).build();
    }

    private String readWriteTokenForNewUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userService.createUser(
            "async" + suffix, "async" + suffix + "@example.com", "hashed", "Async User " + suffix,
            AuthProvider.EMAIL);
        return tokenService.create(
                user.getId(), new CreatePersonalAccessTokenRequest("async-test", TokenScope.READ_WRITE, null))
            .token();
    }

    @Test
    @DisplayName("requisição autenticada (PAT) permanece autorizada após o dispatch ASYNC")
    void authenticatedRequestStaysAuthorizedThroughAsyncDispatch() throws Exception {
        MockMvc mockMvc = mockMvcWithSecurity();
        String token = readWriteTokenForNewUser();

        MvcResult started = mockMvc.perform(get("/api/__async_probe").header("Authorization", "Bearer " + token))
            .andExpect(request().asyncStarted())
            .andReturn();

        // Segundo passo (AsyncContext.dispatch): sem o fix, o filtro PAT pula o async,
        // o contexto fica anônimo e /api/** nega (401). Com o fix, re-autentica -> 200.
        mockMvc.perform(asyncDispatch(started))
            .andExpect(status().isOk());
    }
}
