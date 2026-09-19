package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void publicEndpointsShouldBeAccessibleWithoutAuth() throws Exception {
        // /api/health deve ser acessível sem autenticação
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        // Preview de convite é público por contrato
        mockMvc.perform(get("/api/lists/join/ABC123XYZ789"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedListEndpointsShouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/lists"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mercado","typeId":1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void joinMutationEndpointShouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/lists/join/ABC123XYZ789"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void securityHeadersAndAuthResponsesAreDefensive() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(result -> {
                var headers = result.getResponse();
                org.assertj.core.api.Assertions.assertThat(headers.getHeader("Referrer-Policy"))
                    .isEqualTo("no-referrer");
                org.assertj.core.api.Assertions.assertThat(headers.getHeader("X-Content-Type-Options"))
                    .isEqualTo("nosniff");
                org.assertj.core.api.Assertions.assertThat(headers.getHeader("X-Frame-Options"))
                    .isEqualTo("DENY");
                org.assertj.core.api.Assertions.assertThat(headers.getHeader("Permissions-Policy"))
                    .contains("camera=()", "geolocation=()", "microphone=()");
                org.assertj.core.api.Assertions.assertThat(headers.getHeader("Content-Security-Policy"))
                    .isEqualTo("base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'");
            });

        mockMvc.perform(get("/oauth2/authorization/google"))
            .andExpect(status().is3xxRedirection())
            .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store"));
    }

}
