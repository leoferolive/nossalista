package br.com.leoferolive.nossalista.list.controller;

import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static br.com.leoferolive.nossalista.support.SessionCookieRequestPostProcessor.session;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita o caminho real de autenticação de sessão pela cadeia de filtros.
 * JWTs enviados em Authorization não são aceitos como sessão web.
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
    private String sessionToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        SecurityContextHolder.clearContext();

        testUser = userService.createUser(
                "sessionuser",
                "sessionuser@example.com",
                "hashedPassword",
                "Session User",
                AuthProvider.EMAIL
        );
        sessionToken = jwtService.generateToken(testUser);
    }

    @Test
    @DisplayName("GET /api/lists com cookie de sessão real retorna 200")
    void getAllListsWithSessionCookieReturns200() throws Exception {
        mockMvc.perform(get("/api/lists").with(session(sessionToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/lists/{id} com cookie de sessão real retorna 200")
    void getListByIdWithSessionCookieReturns200() throws Exception {
        SharedList list = listService.createList(new CreateListRequest("Lista Session", 1), testUser);

        mockMvc.perform(get("/api/lists/{id}", list.getId()).with(session(sessionToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/lists rejeita JWT de sessão no Authorization")
    void getAllListsRejectsSessionJwtInAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/lists").header("Authorization", "Bearer " + sessionToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/lists exige CSRF para sessão cookie")
    void createListRequiresCsrfForSessionCookie() throws Exception {
        String body = "{\"name\":\"Lista CSRF\",\"typeId\":1}";

        mockMvc.perform(post("/api/lists")
                .cookie(new Cookie("nl_session", sessionToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/lists")
                .with(session(sessionToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());
    }
}
