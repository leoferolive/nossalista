package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpointsShouldBeAccessibleWithoutAuth() throws Exception {
        // /api/health deve ser acessível sem autenticação
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsShouldReturn401() throws Exception {
        // /api/** (exceto /api/auth/** e /api/health) deve exigir autenticação
        mockMvc.perform(get("/api/lists"))
                .andExpect(status().isUnauthorized());
    }

}
