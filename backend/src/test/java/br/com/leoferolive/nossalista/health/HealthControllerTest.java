package br.com.leoferolive.nossalista.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "app.build.version=v9.9.9",
        "app.build.git-sha=abc123def456",
        "app.build.git-tag=v9.9.9",
        "app.build.environment=ci",
        "app.build.build-time=2026-03-10T12:00:00Z"
})
class HealthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void healthEndpointShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("NossaLista API"))
                .andExpect(jsonPath("$.version").value("v9.9.9"))
                .andExpect(jsonPath("$.gitSha").value("abc123def456"))
                .andExpect(jsonPath("$.gitTag").value("v9.9.9"))
                .andExpect(jsonPath("$.environment").value("ci"))
                .andExpect(jsonPath("$.buildTime").value("2026-03-10T12:00:00Z"));
    }

}
