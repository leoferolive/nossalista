package br.com.leoferolive.nossalista.mcpoauth.controller;

import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthTokenException;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Testes unitários (mocks) de {@link McpOAuthTokenController} chamando
 * {@code token(...)} diretamente — cobre as validações de parâmetro
 * ({@code client_id}, {@code grant_type}, e os requeridos por grant type) que
 * {@code McpOAuthFlowIntegrationTest} não exercita porque sempre monta um
 * corpo de formulário bem-formado.
 */
@ExtendWith(MockitoExtension.class)
class McpOAuthTokenControllerTest {

    @Mock
    private McpOAuthTokenService tokenService;

    private McpOAuthTokenController controller;

    @BeforeEach
    void setUp() {
        controller = new McpOAuthTokenController(tokenService);
    }

    @Test
    @DisplayName("client_id ausente (nulo) é rejeitado com invalid_request, sem chamar o serviço")
    void nullClientIdIsRejected() {
        assertThatThrownBy(() -> controller.token(
            "authorization_code", "code", "http://localhost/callback", null, "verifier", null, null))
            .isInstanceOf(OAuthTokenException.class)
            .hasMessageContaining("client_id");

        verifyNoInteractions(tokenService);
    }

    @Test
    @DisplayName("client_id em branco é rejeitado com invalid_request, sem chamar o serviço")
    void blankClientIdIsRejected() {
        assertThatThrownBy(() -> controller.token(
            "authorization_code", "code", "http://localhost/callback", "   ", "verifier", null, null))
            .isInstanceOf(OAuthTokenException.class)
            .hasMessageContaining("client_id");

        verifyNoInteractions(tokenService);
    }

    @Test
    @DisplayName("grant_type desconhecido é rejeitado com unsupported_grant_type")
    void unknownGrantTypeIsRejected() {
        assertThatThrownBy(() -> controller.token(
            "client_credentials", null, null, "claude-code", null, null, null))
            .isInstanceOf(OAuthTokenException.class)
            .hasMessageContaining("Supported grant types");

        verifyNoInteractions(tokenService);
    }

    @Test
    @DisplayName("authorization_code sem 'code' é rejeitado com invalid_request antes de chamar o serviço")
    void authorizationCodeGrantRequiresCode() {
        assertThatThrownBy(() -> controller.token(
            "authorization_code", null, "http://localhost/callback", "claude-code", "verifier", null, null))
            .isInstanceOf(OAuthTokenException.class)
            .hasMessageContaining("code");

        verify(tokenService, never()).exchangeAuthorizationCode(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("refresh_token em branco é rejeitado com invalid_request antes de chamar o serviço")
    void refreshTokenGrantRejectsBlankRefreshToken() {
        assertThatThrownBy(() -> controller.token(
            "refresh_token", null, null, "claude-code", null, "   ", null))
            .isInstanceOf(OAuthTokenException.class)
            .hasMessageContaining("refresh_token");

        verify(tokenService, never()).refresh(anyString(), anyString(), any());
    }
}
