package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que o host público do dev (Cloudflare Tunnel) é origem aceita pelo CORS.
 *
 * <p>A lista vem do próprio {@code application-dev.yml}, não de uma cópia, para que o teste
 * falhe se alguém tirar o host do arquivo real. O navegador manda {@code Origin} até nos
 * {@code <script type=module crossorigin>} do próprio domínio; sem o host, o asset volta 403.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class DevCorsOriginsIntegrationTest {

    private static final String DEV_PUBLIC_ORIGIN = "https://nossalista-dev.leoferolive.com.br";

    @DynamicPropertySource
    static void devAllowedOrigins(DynamicPropertyRegistry registry) {
        registry.add("cors.allowed-origins", () -> devAllowedOriginsFromYaml());
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("GET com Origin do host público do dev recebe 200 e Access-Control-Allow-Origin")
    void publicDevOriginIsAllowed() throws Exception {
        mockMvc.perform(get("/api/health").header(HttpHeaders.ORIGIN, DEV_PUBLIC_ORIGIN))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEV_PUBLIC_ORIGIN));
    }

    @Test
    @DisplayName("GET com Origin fora da lista continua recebendo 403")
    void unlistedOriginIsRejected() throws Exception {
        mockMvc.perform(get("/api/health").header(HttpHeaders.ORIGIN, "https://origem-nao-listada.example"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a fonte de CORS usada pelo WebSocket/STOMP também aceita o host público do dev")
    void websocketSourceAllowsPublicDevOrigin() {
        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) corsConfigurationSource)
            .getCorsConfigurations()
            .get("/**");

        assertThat(config.getAllowedOrigins()).contains(DEV_PUBLIC_ORIGIN);
    }

    private static String devAllowedOriginsFromYaml() {
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application-dev", new ClassPathResource("application-dev.yml"));
            return String.valueOf(sources.get(0).getProperty("cors.allowed-origins"));
        } catch (IOException e) {
            throw new IllegalStateException("application-dev.yml não pôde ser lido", e);
        }
    }
}
