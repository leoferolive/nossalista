package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void publicEndpointsShouldBeAccessibleWithoutAuth() throws Exception {
        // /api/health deve ser acessível sem autenticação
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    @Disabled("Endpoint /api/lists ainda não implementado - será criado em story futura")
    void protectedEndpointsShouldReturn401() throws Exception {
        // /api/** (exceto /api/auth/** e /api/health) deve exigir autenticação
        mockMvc.perform(get("/api/lists"))
                .andExpect(status().isUnauthorized());
    }

}
