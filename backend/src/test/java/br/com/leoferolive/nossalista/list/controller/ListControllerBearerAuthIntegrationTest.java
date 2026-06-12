package br.com.leoferolive.nossalista.list.controller;

import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REPRODUCAO: exercita o caminho REAL de autenticacao via token Bearer pela
 * cadeia de filtros (JwtAuthenticationFilter), que os testes existentes nao
 * cobrem (eles setam o SecurityContext na mao). Reproduz o 401 de producao.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class ListControllerBearerAuthIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private ListService listService;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private User testUser;
    private String bearer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        SecurityContextHolder.clearContext();

        testUser = userService.createUser(
                "beareruser",
                "beareruser@example.com",
                "hashedPassword",
                "Bearer User",
                AuthProvider.EMAIL
        );
        bearer = "Bearer " + jwtService.generateToken(testUser);
    }

    @Test
    @DisplayName("GET /api/lists com token Bearer real deve retornar 200")
    void getAllListsWithRealBearerTokenReturns200() throws Exception {
        mockMvc.perform(get("/api/lists").header("Authorization", bearer))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/lists/{id} com token Bearer real deve retornar 200 (repro do 401)")
    void getListByIdWithRealBearerTokenReturns200() throws Exception {
        SharedList list = listService.createList(new CreateListRequest("Lista Bearer", 1), testUser);

        mockMvc.perform(get("/api/lists/{id}", list.getId()).header("Authorization", bearer))
                .andExpect(status().isOk());
    }
}
