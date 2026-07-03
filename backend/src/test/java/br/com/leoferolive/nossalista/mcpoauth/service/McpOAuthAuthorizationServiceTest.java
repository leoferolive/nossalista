package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.domain.PendingAuthorization;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthConsentForbiddenException;
import br.com.leoferolive.nossalista.mcpoauth.exception.PendingAuthorizationNotFoundException;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthCodeRepository;
import br.com.leoferolive.nossalista.mcpoauth.repository.PendingAuthorizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (mocks) dos ramos de {@link McpOAuthAuthorizationService}
 * não alcançáveis via {@code McpOAuthFlowIntegrationTest} — TTL real de
 * {@link PendingAuthorization} não expira dentro da janela de um teste, a
 * corrida de reivindicação "perdida sem dono rastreável" exige forçar o mock
 * do UPDATE condicional, e a construção da URL de redirect
 * ({@link McpOAuthAuthorizationService#approve}) tem ramos (separador já
 * presente no redirect_uri, parâmetro nulo) que os clientes registrados nos
 * testes de integração nunca exercitam.
 */
@ExtendWith(MockitoExtension.class)
class McpOAuthAuthorizationServiceTest {

    @Mock
    private McpOAuthProperties properties;

    @Mock
    private McpOAuthClientRegistry clientRegistry;

    @Mock
    private PendingAuthorizationRepository pendingRepository;

    @Mock
    private McpOAuthCodeRepository codeRepository;

    @InjectMocks
    private McpOAuthAuthorizationService authorizationService;

    private static final String CLIENT_ID = "claude-code";
    private static final String NONCE = "correct-nonce-value";

    private PendingAuthorization newPending(String redirectUri, String state) {
        PendingAuthorization pending = new PendingAuthorization();
        pending.setId(UUID.randomUUID());
        pending.setClientId(CLIENT_ID);
        pending.setRedirectUri(redirectUri);
        pending.setScope(TokenScope.READ);
        pending.setState(state);
        pending.setCodeChallenge("challenge");
        pending.setResource("http://localhost:8080/mcp");
        pending.setNonce(NONCE);
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        return pending;
    }

    @Test
    @DisplayName("view: pedido pendente expirado é removido e rejeitado como inexistente")
    void viewRejectsExpiredPendingAuthorization() {
        PendingAuthorization pending = newPending("http://localhost:8765/callback", "s");
        pending.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(pendingRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> authorizationService.view(pending.getId(), UUID.randomUUID()))
            .isInstanceOf(PendingAuthorizationNotFoundException.class)
            .hasMessageContaining("expired");

        verify(pendingRepository).delete(pending);
    }

    @Test
    @DisplayName("claimOrVerifyOwnership: perde a corrida de reivindicação e o dono não é sequer rastreável")
    void viewRejectsWhenClaimRaceLostAndOwnerUntraceable() {
        PendingAuthorization pending = newPending("http://localhost:8765/callback", "s");
        UUID userId = UUID.randomUUID();
        when(pendingRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(pendingRepository.claimIfUnclaimed(pending.getId(), userId)).thenReturn(0);
        when(pendingRepository.findClaimedByUserId(pending.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.view(pending.getId(), userId))
            .isInstanceOf(OAuthConsentForbiddenException.class);
    }

    @Test
    @DisplayName("approve: cookie de vínculo com valor incorreto (não ausente) é rejeitado")
    void approveRejectsWrongConsentCookieValue() {
        PendingAuthorization pending = newPending("http://localhost:8765/callback", "s");
        when(pendingRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> authorizationService.approve(pending.getId(), UUID.randomUUID(), "wrong-nonce-value"))
            .isInstanceOf(OAuthConsentForbiddenException.class);

        verify(pendingRepository, never()).delete(any());
    }

    @Test
    @DisplayName("approve: redirect_uri que já contém query string usa '&' em vez de '?' para anexar code/state")
    void approveAppendsWithAmpersandWhenRedirectUriAlreadyHasQueryString() {
        PendingAuthorization pending = newPending("http://localhost:8765/callback?existing=param", "state-1");
        UUID userId = UUID.randomUUID();
        when(pendingRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(pendingRepository.claimIfUnclaimed(pending.getId(), userId)).thenReturn(1);

        String redirectUrl = authorizationService.approve(pending.getId(), userId, NONCE);

        assertThat(redirectUrl).startsWith("http://localhost:8765/callback?existing=param&code=");
        assertThat(redirectUrl).contains("&state=state-1");
        verify(pendingRepository).delete(pending);
    }

    @Test
    @DisplayName("deny: state ausente (nulo) não é anexado à URL de redirect")
    void denyOmitsNullStateFromRedirectUrl() {
        PendingAuthorization pending = newPending("http://localhost:8765/callback", null);
        UUID userId = UUID.randomUUID();
        when(pendingRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(pendingRepository.claimIfUnclaimed(pending.getId(), userId)).thenReturn(1);

        String redirectUrl = authorizationService.deny(pending.getId(), userId, NONCE);

        assertThat(redirectUrl).isEqualTo("http://localhost:8765/callback?error=access_denied");
        assertThat(redirectUrl).doesNotContain("state=");
    }
}
