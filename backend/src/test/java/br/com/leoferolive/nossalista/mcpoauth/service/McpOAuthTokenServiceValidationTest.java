package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties.ClientDefinition;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthCode;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRefreshToken;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthTokenException;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthCodeRepository;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthRefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (mocks) dos ramos de VALIDAÇÃO de {@link McpOAuthTokenService}
 * não cobertos por {@link McpOAuthTokenServiceTest} (corridas atômicas) nem por
 * {@code McpOAuthFlowIntegrationTest} (fluxo feliz + validações alcançáveis via
 * HTTP com TTLs reais). Complementa especificamente: code/refresh token
 * expirados, client_id divergente do vinculado à credencial, o parâmetro
 * {@code resource} omitido (RFC 8707: ausência não é mismatch), e as
 * ramificações silenciosas de {@code /oauth/revoke} (RFC 7009).
 */
@ExtendWith(MockitoExtension.class)
class McpOAuthTokenServiceValidationTest {

    @Mock
    private McpOAuthProperties properties;

    @Mock
    private McpOAuthClientRegistry clientRegistry;

    @Mock
    private McpOAuthCodeRepository codeRepository;

    @Mock
    private McpOAuthRefreshTokenRepository refreshTokenRepository;

    @Mock
    private McpOAuthJwtService jwtService;

    @Mock
    private PkceValidator pkceValidator;

    @InjectMocks
    private McpOAuthTokenService tokenService;

    private static final String CLIENT_ID = "claude-code";
    private static final String REDIRECT_URI = "http://localhost:8765/callback";
    private static final String RESOURCE = "http://localhost:8080/mcp";

    private McpOAuthCode activeCode(String rawCode) {
        McpOAuthCode entity = new McpOAuthCode();
        entity.setId(UUID.randomUUID());
        entity.setCode(rawCode);
        entity.setUserId(UUID.randomUUID());
        entity.setClientId(CLIENT_ID);
        entity.setRedirectUri(REDIRECT_URI);
        entity.setScope(TokenScope.READ);
        entity.setCodeChallenge("challenge");
        entity.setResource(RESOURCE);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        return entity;
    }

    private McpOAuthRefreshToken activeRefreshToken() {
        McpOAuthRefreshToken existing = new McpOAuthRefreshToken();
        existing.setId(UUID.randomUUID());
        existing.setTokenHash("irrelevant-para-este-mock");
        existing.setFamilyId(UUID.randomUUID());
        existing.setUserId(UUID.randomUUID());
        existing.setClientId(CLIENT_ID);
        existing.setScope(TokenScope.READ);
        existing.setResource(RESOURCE);
        existing.setExpiresAt(LocalDateTime.now().plusDays(1));
        return existing;
    }

    // ------------------------------------------------------- code validation

    @Test
    @DisplayName("exchangeAuthorizationCode: code expirado é rejeitado (invalid_grant)")
    void exchangeAuthorizationCodeRejectsExpiredCode() {
        McpOAuthCode entity = activeCode("expired-code");
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(clientRegistry.require(CLIENT_ID)).thenReturn(new ClientDefinition());
        when(codeRepository.findByCode("expired-code")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> tokenService.exchangeAuthorizationCode(
            "expired-code", REDIRECT_URI, CLIENT_ID, "verifier", RESOURCE))
            .isInstanceOf(OAuthTokenException.class)
            .satisfies(ex -> {
                OAuthTokenException oauthEx = (OAuthTokenException) ex;
                assertThat(oauthEx.getErrorCode()).isEqualTo("invalid_grant");
                assertThat(oauthEx.getMessage()).contains("expired");
            });

        verify(codeRepository, never()).markConsumed(any(), any(), any());
    }

    @Test
    @DisplayName("exchangeAuthorizationCode: client_id divergente do vinculado ao code é rejeitado")
    void exchangeAuthorizationCodeRejectsClientIdMismatch() {
        McpOAuthCode entity = activeCode("mismatched-client-code");

        when(clientRegistry.require("other-client")).thenReturn(new ClientDefinition());
        when(codeRepository.findByCode("mismatched-client-code")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> tokenService.exchangeAuthorizationCode(
            "mismatched-client-code", REDIRECT_URI, "other-client", "verifier", RESOURCE))
            .isInstanceOf(OAuthTokenException.class)
            .satisfies(ex -> {
                OAuthTokenException oauthEx = (OAuthTokenException) ex;
                assertThat(oauthEx.getErrorCode()).isEqualTo("invalid_grant");
                assertThat(oauthEx.getMessage()).contains("client_id");
            });
    }

    @Test
    @DisplayName("exchangeAuthorizationCode: resource omitido no pedido não é tratado como mismatch (RFC 8707)")
    void exchangeAuthorizationCodeAllowsOmittedResource() {
        McpOAuthCode entity = activeCode("resource-omitted-code");
        UUID familyId = UUID.randomUUID();

        when(clientRegistry.require(CLIENT_ID)).thenReturn(new ClientDefinition());
        when(codeRepository.findByCode("resource-omitted-code")).thenReturn(Optional.of(entity));
        when(pkceValidator.matches(anyString(), eq("challenge"))).thenReturn(true);
        when(codeRepository.markConsumed(eq(entity.getId()), any(UUID.class), any(LocalDateTime.class))).thenReturn(1);
        when(jwtService.issueAccessToken(any(), any(), any(), any())).thenReturn("access-token");
        when(jwtService.accessTokenTtl()).thenReturn(Duration.ofMinutes(10));

        // resource=null no pedido, embora o code esteja vinculado a RESOURCE — não deve lançar.
        assertThat(tokenService.exchangeAuthorizationCode(
            "resource-omitted-code", REDIRECT_URI, CLIENT_ID, "verifier", null).accessToken())
            .isEqualTo("access-token");
    }

    @Test
    @DisplayName("rejectIfReplayed: code consumido sem família de tokens associada não tenta revogar nenhuma família")
    void replayedCodeWithoutTokenFamilyDoesNotRevokeAnything() {
        McpOAuthCode entity = activeCode("replayed-without-family");
        entity.setConsumedAt(LocalDateTime.now().minusSeconds(5));
        // tokenFamilyId permanece null: este code foi consumido mas nenhum par de
        // tokens chegou a ser emitido por ele (cenário defensivo).

        when(clientRegistry.require(CLIENT_ID)).thenReturn(new ClientDefinition());
        when(codeRepository.findByCode("replayed-without-family")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> tokenService.exchangeAuthorizationCode(
            "replayed-without-family", REDIRECT_URI, CLIENT_ID, "verifier", RESOURCE))
            .isInstanceOf(OAuthTokenException.class)
            .satisfies(ex -> assertThat(((OAuthTokenException) ex).getMessage()).contains("replay"));

        verify(refreshTokenRepository, never()).revokeFamily(any(), any());
    }

    @Test
    @DisplayName("rejectConcurrentCodeReplay: UPDATE perde a corrida mas a família vencedora já não é rastreável")
    void concurrentReplayWithoutTraceableFamilyDoesNotRevokeAnything() {
        McpOAuthCode entity = activeCode("concurrent-untraceable-code");

        when(clientRegistry.require(CLIENT_ID)).thenReturn(new ClientDefinition());
        when(codeRepository.findByCode("concurrent-untraceable-code")).thenReturn(Optional.of(entity));
        when(pkceValidator.matches(anyString(), eq("challenge"))).thenReturn(true);
        when(codeRepository.markConsumed(eq(entity.getId()), any(UUID.class), any(LocalDateTime.class))).thenReturn(0);
        when(codeRepository.findTokenFamilyIdByCode("concurrent-untraceable-code")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.exchangeAuthorizationCode(
            "concurrent-untraceable-code", REDIRECT_URI, CLIENT_ID, "verifier", RESOURCE))
            .isInstanceOf(OAuthTokenException.class)
            .satisfies(ex -> assertThat(((OAuthTokenException) ex).getMessage()).contains("replay"));

        verify(refreshTokenRepository, never()).revokeFamily(any(), any());
    }

    // -------------------------------------------------- refresh token validation

    @Test
    @DisplayName("refresh: refresh token expirado é rejeitado (invalid_grant)")
    void refreshRejectsExpiredRefreshToken() {
        McpOAuthRefreshToken existing = activeRefreshToken();
        existing.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(clientRegistry.require(CLIENT_ID)).thenReturn(new ClientDefinition());
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tokenService.refresh("any-refresh-token", CLIENT_ID, RESOURCE))
            .isInstanceOf(OAuthTokenException.class)
            .satisfies(ex -> {
                OAuthTokenException oauthEx = (OAuthTokenException) ex;
                assertThat(oauthEx.getErrorCode()).isEqualTo("invalid_grant");
                assertThat(oauthEx.getMessage()).contains("expired");
            });

        verify(refreshTokenRepository, never()).markRotated(any(), any());
    }

    @Test
    @DisplayName("refresh: client_id divergente do vinculado ao refresh token é rejeitado")
    void refreshRejectsClientIdMismatch() {
        McpOAuthRefreshToken existing = activeRefreshToken();

        when(clientRegistry.require("other-client")).thenReturn(new ClientDefinition());
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tokenService.refresh("any-refresh-token", "other-client", RESOURCE))
            .isInstanceOf(OAuthTokenException.class)
            .satisfies(ex -> {
                OAuthTokenException oauthEx = (OAuthTokenException) ex;
                assertThat(oauthEx.getErrorCode()).isEqualTo("invalid_grant");
                assertThat(oauthEx.getMessage()).contains("client_id");
            });
    }

    // -------------------------------------------------------------- revoke()

    @Test
    @DisplayName("revoke: token nulo é ignorado silenciosamente (RFC 7009), sem tocar o repositório")
    void revokeIgnoresNullToken() {
        tokenService.revoke(null, CLIENT_ID);

        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }

    @Test
    @DisplayName("revoke: token em branco é ignorado silenciosamente, sem tocar o repositório")
    void revokeIgnoresBlankToken() {
        tokenService.revoke("   ", CLIENT_ID);

        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }

    @Test
    @DisplayName("revoke: client_id omitido revoga mesmo assim (revogação sem checagem de dono)")
    void revokeWithNullClientIdRevokesRegardless() {
        McpOAuthRefreshToken existing = activeRefreshToken();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        tokenService.revoke("some-token", null);

        verify(refreshTokenRepository).revokeFamily(eq(existing.getFamilyId()), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("revoke: client_id divergente do dono do token NÃO revoga (RFC 7009 não vaza validade)")
    void revokeWithMismatchedClientIdDoesNotRevoke() {
        McpOAuthRefreshToken existing = activeRefreshToken();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        tokenService.revoke("some-token", "a-completely-different-client");

        verify(refreshTokenRepository, never()).revokeFamily(any(), any());
    }
}
