package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que @EnableMethodSecurity está ativo e que @PreAuthorize("hasRole('ADMIN')")
 * é efetivamente aplicado: ADMIN acessa, USER e anônimo são barrados.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("RBAC com method security")
class MethodSecurityRbacTest {

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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN acessa endpoint protegido por hasRole('ADMIN')")
    void adminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/test-only/admin-area"))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-ok"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER é barrado (403) no endpoint protegido por hasRole('ADMIN')")
    void userCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/test-only/admin-area"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("anônimo é barrado no endpoint protegido por hasRole('ADMIN')")
    void anonymousCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/test-only/admin-area"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER continua acessando endpoint sem restrição de role")
    void userCanAccessUnrestrictedEndpoint() throws Exception {
        mockMvc.perform(get("/test-only/open-area"))
                .andExpect(status().isOk())
                .andExpect(content().string("open-ok"));
    }

    @TestConfiguration
    static class TestControllerConfig {
        @Bean
        TestOnlyEndpoints testOnlyEndpoints() {
            return new TestOnlyEndpoints();
        }
    }

    @RestController
    static class TestOnlyEndpoints {

        @GetMapping("/test-only/admin-area")
        @PreAuthorize("hasRole('ADMIN')")
        String adminArea() {
            return "admin-ok";
        }

        @GetMapping("/test-only/open-area")
        String openArea() {
            return "open-ok";
        }
    }
}
