package br.com.leoferolive.nossalista.mcpoauth.controller;

import br.com.leoferolive.nossalista.config.ClientIpResolver;
import br.com.leoferolive.nossalista.config.RateLimiterService;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.dto.ClientRegistrationRequest;
import br.com.leoferolive.nossalista.mcpoauth.dto.ClientRegistrationResponse;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthClientRegistrationException;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthDynamicClientService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (mocks) de {@link McpOAuthClientRegistrationController} —
 * o flag {@code app.mcp-oauth.dcr.enabled} e o rate limit por IP, sem subir
 * contexto Spring nem banco (o fluxo feliz ponta-a-ponta fica em
 * {@code McpOAuthDynamicClientRegistrationIntegrationTest}).
 */
@ExtendWith(MockitoExtension.class)
class McpOAuthClientRegistrationControllerTest {

    @Mock
    private McpOAuthDynamicClientService clientService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private ClientIpResolver clientIpResolver;

    @Mock
    private HttpServletRequest httpServletRequest;

    private McpOAuthProperties properties;
    private McpOAuthClientRegistrationController controller;

    @BeforeEach
    void setUp() {
        properties = new McpOAuthProperties();
        controller = new McpOAuthClientRegistrationController(clientService, properties, rateLimiterService, clientIpResolver);
    }

    @Test
    @DisplayName("DCR desabilitado (app.mcp-oauth.dcr.enabled=false) responde access_denied, sem chamar o serviço")
    void registerRejectsWhenDcrDisabled() {
        properties.getDcr().setEnabled(false);

        assertThatThrownBy(() -> controller.register(validRequest(), httpServletRequest))
            .isInstanceOf(OAuthClientRegistrationException.class)
            .satisfies(ex -> assertThat(((OAuthClientRegistrationException) ex).getErrorCode()).isEqualTo("access_denied"));

        verifyNoInteractions(clientService, rateLimiterService);
    }

    @Test
    @DisplayName("rate limit por IP excedido responde too_many_requests, sem chamar o serviço de registro")
    void registerRejectsWhenRateLimitExceeded() {
        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("203.0.113.1");
        when(rateLimiterService.isAllowed(anyString(), anyInt(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> controller.register(validRequest(), httpServletRequest))
            .isInstanceOf(OAuthClientRegistrationException.class)
            .satisfies(ex -> assertThat(((OAuthClientRegistrationException) ex).getErrorCode())
                .isEqualTo("too_many_requests"));

        verifyNoInteractions(clientService);
    }

    @Test
    @DisplayName("pedido válido dentro do rate limit delega ao serviço e responde 201")
    void registerDelegatesToServiceAndReturns201() {
        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("203.0.113.1");
        when(rateLimiterService.isAllowed(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        ClientRegistrationResponse expected = new ClientRegistrationResponse(
            "dcr_abc123", 0L, List.of("https://app.example.com/callback"), "none",
            List.of("authorization_code", "refresh_token"), List.of("code"), "read read_write", null);
        when(clientService.register(any())).thenReturn(expected);

        ResponseEntity<ClientRegistrationResponse> response = controller.register(validRequest(), httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    private ClientRegistrationRequest validRequest() {
        return new ClientRegistrationRequest(
            List.of("https://app.example.com/callback"), null, null, null, null, null, null);
    }
}
