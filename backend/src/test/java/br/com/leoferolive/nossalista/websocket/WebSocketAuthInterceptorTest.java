package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketAuthInterceptor Tests")
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    private WebSocketAuthInterceptor interceptor;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtService, userService);
    }

    @Test
    @DisplayName("Token válido no query param deve autenticar e retornar true")
    void shouldAuthenticateWithValidTokenInQueryParam() throws Exception {
        User user = new User();
        user.setId(USER_ID);

        when(request.getURI()).thenReturn(URI.create("/ws?token=" + VALID_TOKEN));
        when(jwtService.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.findById(USER_ID)).thenReturn(Optional.of(user));

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes.get("user")).isEqualTo(user);
    }

    @Test
    @DisplayName("Token válido no header Authorization deve autenticar e retornar true")
    void shouldAuthenticateWithValidTokenInAuthorizationHeader() throws Exception {
        User user = new User();
        user.setId(USER_ID);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + VALID_TOKEN);

        when(request.getURI()).thenReturn(URI.create("/ws"));
        when(request.getHeaders()).thenReturn(headers);
        when(jwtService.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.findById(USER_ID)).thenReturn(Optional.of(user));

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes.get("user")).isEqualTo(user);
    }

    @Test
    @DisplayName("Token inválido deve rejeitar conexão retornando false")
    void shouldRejectWithInvalidToken() throws Exception {
        when(request.getURI()).thenReturn(URI.create("/ws?token=invalid.token"));
        when(jwtService.validateToken("invalid.token")).thenReturn(false);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Sem token deve rejeitar conexão retornando false")
    void shouldRejectWithoutToken() throws Exception {
        when(request.getURI()).thenReturn(URI.create("/ws"));
        when(request.getHeaders()).thenReturn(HttpHeaders.EMPTY);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Token válido mas usuário não encontrado deve rejeitar conexão")
    void shouldRejectWhenUserNotFound() throws Exception {
        when(request.getURI()).thenReturn(URI.create("/ws?token=" + VALID_TOKEN));
        when(jwtService.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.findById(USER_ID)).thenReturn(Optional.empty());

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("afterHandshake não deve lançar exceção")
    void afterHandshakeShouldNotThrow() {
        // afterHandshake é no-op, apenas verifica que não lança exceção
        interceptor.afterHandshake(request, response, wsHandler, null);
    }
}
