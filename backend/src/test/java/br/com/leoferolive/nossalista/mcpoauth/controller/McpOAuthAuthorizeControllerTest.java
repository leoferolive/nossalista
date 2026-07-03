package br.com.leoferolive.nossalista.mcpoauth.controller;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties.ClientDefinition;
import br.com.leoferolive.nossalista.mcpoauth.domain.PendingAuthorization;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthAuthorizationService;
import br.com.leoferolive.nossalista.mcpoauth.service.McpOAuthClientRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (mocks) de {@link McpOAuthAuthorizeController} chamando
 * {@code authorize(...)} diretamente (sem MockMvc/binding do Spring) — permite
 * exercitar combinações de parâmetros que o binding de
 * {@code @RequestParam}/{@code defaultValue} nunca produziria isoladamente via
 * HTTP real (ex.: {@code scope} nulo, {@code state} nulo) e redirect_uris com
 * query string pré-existente sem depender de um cliente registrado com esse
 * formato — {@link McpOAuthClientRegistry} é mockado, então a validação real de
 * client/redirect_uri (coberta em {@code McpOAuthClientRegistryTest} e em
 * {@code McpOAuthFlowIntegrationTest}) não entra em jogo aqui.
 */
@ExtendWith(MockitoExtension.class)
class McpOAuthAuthorizeControllerTest {

    @Mock
    private McpOAuthClientRegistry clientRegistry;

    @Mock
    private McpOAuthAuthorizationService authorizationService;

    @Mock
    private McpOAuthProperties properties;

    @Mock
    private HttpServletResponse httpResponse;

    private McpOAuthAuthorizeController controller;

    private static final String CLIENT_ID = "claude-code";
    private static final String RESOURCE = "http://localhost:8080/mcp";

    @BeforeEach
    void setUp() {
        controller = new McpOAuthAuthorizeController(clientRegistry, authorizationService, properties);
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://localhost:5173");
        when(clientRegistry.require(CLIENT_ID)).thenReturn(new ClientDefinition());
        doNothing().when(clientRegistry).validateRedirectUri(any(), anyString());
        // Só alcançado pelos testes cujas validações anteriores (state/response_type/
        // code_challenge) passam — lenient() evita UnnecessaryStubbingException nos
        // demais, que retornam erro antes de chegar à checagem de resource.
        lenient().when(properties.getResource()).thenReturn(RESOURCE);
    }

    private String redirectedLocation() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpResponse).sendRedirect(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("state ausente (nulo) não é validado por tamanho e não é ecoado num erro subsequente")
    void nullStateSkipsLengthCheckAndIsOmittedFromErrorRedirect() throws Exception {
        // response_type inválido dispara o redirect de erro; usado aqui só para
        // verificar que um state nulo passa incólume pela checagem de tamanho
        // (linha "state != null && state.length() > MAX") e não aparece na URL.
        controller.authorize(
            "token", CLIENT_ID, "http://localhost:8765/callback", "read", null, "challenge", "S256", RESOURCE,
            httpResponse);

        String location = redirectedLocation();
        assertThat(location).contains("error=unsupported_response_type");
        assertThat(location).doesNotContain("&state=");
    }

    @Test
    @DisplayName("state maior que o limite suportado é rejeitado antes de qualquer outra validação")
    void stateExceedingMaxLengthIsRejected() throws Exception {
        String tooLongState = "s".repeat(513);

        controller.authorize(
            "code", CLIENT_ID, "http://localhost:8765/callback", "read", tooLongState, "challenge", "S256", RESOURCE,
            httpResponse);

        String location = redirectedLocation();
        assertThat(location).contains("error=invalid_request");
        assertThat(location).contains("exceeds+the+maximum+supported+length");
        // o state gigante não é ecoado de volta (não seria útil e infla a URL).
        assertThat(location).doesNotContain("&state=" + tooLongState);
    }

    @Test
    @DisplayName("response_type diferente de 'code' é rejeitado com unsupported_response_type")
    void nonCodeResponseTypeIsRejected() throws Exception {
        controller.authorize(
            "token", CLIENT_ID, "http://localhost:8765/callback", "read", "s", "challenge", "S256", RESOURCE,
            httpResponse);

        assertThat(redirectedLocation()).contains("error=unsupported_response_type");
    }

    @Test
    @DisplayName("code_challenge nulo é rejeitado com invalid_request")
    void nullCodeChallengeIsRejected() throws Exception {
        controller.authorize(
            "code", CLIENT_ID, "http://localhost:8765/callback", "read", "s", null, "S256", RESOURCE, httpResponse);

        assertThat(redirectedLocation()).contains("error=invalid_request");
    }

    @Test
    @DisplayName("code_challenge em branco é rejeitado com invalid_request")
    void blankCodeChallengeIsRejected() throws Exception {
        controller.authorize(
            "code", CLIENT_ID, "http://localhost:8765/callback", "read", "s", "   ", "S256", RESOURCE, httpResponse);

        assertThat(redirectedLocation()).contains("error=invalid_request");
    }

    @Test
    @DisplayName("scope nulo é rejeitado com invalid_scope")
    void nullScopeIsRejected() throws Exception {
        controller.authorize(
            "code", CLIENT_ID, "http://localhost:8765/callback", null, "s", "challenge", "S256", RESOURCE,
            httpResponse);

        assertThat(redirectedLocation()).contains("error=invalid_scope");
    }

    @Test
    @DisplayName("scope desconhecido (nem read nem read_write) é rejeitado com invalid_scope")
    void unknownScopeValueIsRejected() throws Exception {
        controller.authorize(
            "code", CLIENT_ID, "http://localhost:8765/callback", "admin", "s", "challenge", "S256", RESOURCE,
            httpResponse);

        assertThat(redirectedLocation()).contains("error=invalid_scope");
    }

    @Test
    @DisplayName("scope em branco (só espaços) é rejeitado com invalid_scope, mesmo comportamento de scope vazio hoje")
    void blankScopeIsRejected() throws Exception {
        controller.authorize(
            "code", CLIENT_ID, "http://localhost:8765/callback", "   ", "s", "challenge", "S256", RESOURCE,
            httpResponse);

        assertThat(redirectedLocation()).contains("error=invalid_scope");
    }

    @Test
    @DisplayName("scope vazio é rejeitado com invalid_scope (default atual preservado — @RequestParam(defaultValue) "
        + "só se aplica quando o parâmetro está ausente, não quando vem vazio)")
    void emptyScopeIsRejected() throws Exception {
        controller.authorize(
            "code", CLIENT_ID, "http://localhost:8765/callback", "", "s", "challenge", "S256", RESOURCE, httpResponse);

        assertThat(redirectedLocation()).contains("error=invalid_scope");
    }

    @Test
    @DisplayName("scope com um token desconhecido no meio da lista (\"read foo\") é rejeitado com invalid_scope")
    void scopeListWithOneUnknownTokenIsRejected() throws Exception {
        controller.authorize(
            "code", CLIENT_ID, "http://localhost:8765/callback", "read foo", "s", "challenge", "S256", RESOURCE,
            httpResponse);

        assertThat(redirectedLocation()).contains("error=invalid_scope");
    }

    @ParameterizedTest(name = "scope=\"{0}\" concede o escopo efetivo {1}")
    @DisplayName("scope como lista separada por espaço (RFC 6749 §3.3): escopo efetivo concedido é o superset pedido, "
        + "independente da ordem, sem escalonamento além do que foi solicitado")
    @CsvSource({
        "read, READ",
        "read_write, READ_WRITE",
        "'read read_write', READ_WRITE",
        "'read_write read', READ_WRITE",
        "'read read', READ",
    })
    void scopeListGrantsExpectedEffectiveScope(String scopeParam, TokenScope expectedScope) throws Exception {
        PendingAuthorization pending = stubPendingAuthorization();
        ArgumentCaptor<TokenScope> scopeCaptor = ArgumentCaptor.forClass(TokenScope.class);
        when(authorizationService.createPending(
            eq(CLIENT_ID), anyString(), scopeCaptor.capture(), anyString(), anyString(), anyString()))
            .thenReturn(pending);

        controller.authorize(
            "code", CLIENT_ID, "http://localhost:8765/callback", scopeParam, "s", "challenge", "S256", RESOURCE,
            httpResponse);

        assertThat(scopeCaptor.getValue()).isEqualTo(expectedScope);
        assertThat(redirectedLocation()).contains("/oauth/consent?request_id=");
    }

    @Test
    @DisplayName("scope com múltiplos espaços e espaços à volta é tolerado (\"  read   read_write  \" -> read_write)")
    void scopeWithExtraAndSurroundingWhitespaceIsTolerated() throws Exception {
        PendingAuthorization pending = stubPendingAuthorization();
        ArgumentCaptor<TokenScope> scopeCaptor = ArgumentCaptor.forClass(TokenScope.class);
        when(authorizationService.createPending(
            eq(CLIENT_ID), anyString(), scopeCaptor.capture(), anyString(), anyString(), anyString()))
            .thenReturn(pending);

        controller.authorize(
            "code", CLIENT_ID, "http://localhost:8765/callback", "  read   read_write  ", "s", "challenge", "S256",
            RESOURCE, httpResponse);

        assertThat(scopeCaptor.getValue()).isEqualTo(TokenScope.READ_WRITE);
        assertThat(redirectedLocation()).contains("/oauth/consent?request_id=");
    }

    private PendingAuthorization stubPendingAuthorization() {
        lenient().when(properties.getPendingAuthorizationTtl()).thenReturn(Duration.ofMinutes(10));
        PendingAuthorization pending = new PendingAuthorization();
        pending.setId(UUID.randomUUID());
        pending.setNonce("nonce");
        return pending;
    }

    @Test
    @DisplayName("erro de validação com redirect_uri que já contém query string usa '&' em vez de '?'")
    void errorRedirectAppendsWithAmpersandWhenRedirectUriAlreadyHasQueryString() throws Exception {
        controller.authorize(
            "token", CLIENT_ID, "http://localhost:8765/callback?already=here", "read", "s", "challenge", "S256",
            RESOURCE, httpResponse);

        String location = redirectedLocation();
        assertThat(location).startsWith("http://localhost:8765/callback?already=here&error=");
    }
}
