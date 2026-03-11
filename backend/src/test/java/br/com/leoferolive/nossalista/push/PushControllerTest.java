package br.com.leoferolive.nossalista.push;

import br.com.leoferolive.nossalista.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PushController Tests")
class PushControllerTest {

    @Mock
    private PushSubscriptionStore subscriptionStore;

    @Mock
    private VapidConfig vapidConfig;

    private MockMvc mockMvc;
    private User user;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PushController controller = new PushController(subscriptionStore, vapidConfig);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())
        );
    }

    @Test
    @DisplayName("GET /vapid-public-key retorna 204 quando VAPID não está configurado")
    void getVapidPublicKey_returnsNoContent_whenNotConfigured() throws Exception {
        when(vapidConfig.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/api/push/vapid-public-key"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /vapid-public-key retorna chave pública quando configurado")
    void getVapidPublicKey_returnsKey_whenConfigured() throws Exception {
        when(vapidConfig.isConfigured()).thenReturn(true);
        when(vapidConfig.getPublicKey()).thenReturn("minha-chave-publica");

        mockMvc.perform(get("/api/push/vapid-public-key"))
            .andExpect(status().isOk())
            .andExpect(content().string("minha-chave-publica"));
    }

    @Test
    @DisplayName("POST /subscribe adiciona subscrição para o usuário autenticado")
    void subscribe_addsSubscription_forAuthenticatedUser() throws Exception {
        PushSubscription subscription = new PushSubscription("https://push.example.com/sub", "p256dh", "auth");

        mockMvc.perform(post("/api/push/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscription))
                .principal(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())))
            .andExpect(status().isOk());

        verify(subscriptionStore).add(user.getId(), subscription);
    }

    @Test
    @DisplayName("DELETE /unsubscribe remove subscrição para o usuário autenticado")
    void unsubscribe_removesSubscription_forAuthenticatedUser() throws Exception {
        String endpoint = "https://push.example.com/sub";
        String body = "{\"endpoint\":\"" + endpoint + "\"}";

        mockMvc.perform(delete("/api/push/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .principal(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())))
            .andExpect(status().isOk());

        verify(subscriptionStore).remove(user.getId(), endpoint);
    }
}
