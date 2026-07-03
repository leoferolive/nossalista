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
 * Testes unitários (mocks) do caminho de CORRIDA do consumo/rotação atômicos
 * (achado do review, I-1/M-1): {@link McpOAuthAtomicUpdatesRepositoryTest}
 * (nível de repositório) já prova que o UPDATE condicional em si é um
 * compare-and-swap correto; estes testes provam que o SERVIÇO reage
 * corretamente quando esse UPDATE perde a corrida (retorna 0 linhas afetadas)
 * — revogando a família certa e nunca emitindo um segundo par de tokens.
 *
 * <p>Esse cenário não é alcançável chamando o endpoint HTTP duas vezes em
 * sequência (o atalho {@code rejectIfReplayed}/{@code rejectIfReused}, baseado
 * no snapshot já lido, sempre intercepta o replay sequencial primeiro) — só
 * ocorre quando duas transações leem o mesmo snapshot "ainda não consumido" e
 * disputam a mesma linha, exatamente o que se simula aqui forçando o mock do
 * UPDATE condicional a devolver 0.</p>
 */
@ExtendWith(MockitoExtension.class)
class McpOAuthTokenServiceTest {

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

    @Test
    @DisplayName("exchangeAuthorizationCode: quando o UPDATE condicional perde a corrida (0 linhas), "
        + "revoga a família VENCEDORA e não emite tokens")
    void exchangeAuthorizationCodeRevokesWinningFamilyOnConcurrentReplay() {
        UUID codeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID winningFamilyId = UUID.randomUUID();
        String rawCode = "atomic-race-code";

        McpOAuthCode entity = new McpOAuthCode();
        entity.setId(codeId);
        entity.setCode(rawCode);
        entity.setUserId(userId);
        entity.setClientId(CLIENT_ID);
        entity.setRedirectUri(REDIRECT_URI);
        entity.setScope(TokenScope.READ);
        entity.setCodeChallenge("challenge");
        entity.setResource(RESOURCE);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        // consumedAt/tokenFamilyId permanecem null: este É o snapshot "ainda não
        // consumido" que as DUAS transações concorrentes leem antes de disputar
        // o UPDATE condicional.

        when(clientRegistry.require(CLIENT_ID)).thenReturn(new ClientDefinition());
        when(codeRepository.findByCode(rawCode)).thenReturn(Optional.of(entity));
        when(pkceValidator.matches(anyString(), eq("challenge"))).thenReturn(true);
        // A OUTRA transação já venceu a corrida entre o SELECT acima e este UPDATE.
        when(codeRepository.markConsumed(eq(codeId), any(UUID.class), any(LocalDateTime.class))).thenReturn(0);
        when(codeRepository.findTokenFamilyIdByCode(rawCode)).thenReturn(Optional.of(winningFamilyId));

        assertThatThrownBy(() -> tokenService.exchangeAuthorizationCode(
            rawCode, REDIRECT_URI, CLIENT_ID, "verifier", RESOURCE))
            .isInstanceOf(OAuthTokenException.class)
            .satisfies(ex -> {
                OAuthTokenException oauthEx = (OAuthTokenException) ex;
                assertThat(oauthEx.getErrorCode()).isEqualTo("invalid_grant");
                assertThat(oauthEx.getMessage()).contains("replay");
            });

        // Revoga a família VENCEDORA (lida do banco), nunca a família local que
        // esta transação perdedora tentou (e nunca chegou a persistir).
        verify(refreshTokenRepository).revokeFamily(eq(winningFamilyId), any(LocalDateTime.class));
        verify(jwtService, never()).issueAccessToken(any(), any(), any(), any());
    }

    @Test
    @DisplayName("refresh: quando o UPDATE condicional perde a corrida (0 linhas), "
        + "revoga a família inteira e não emite tokens")
    void refreshRevokesFamilyOnConcurrentReuse() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        String rawRefreshToken = "atomic-race-refresh-token";

        McpOAuthRefreshToken existing = new McpOAuthRefreshToken();
        existing.setId(tokenId);
        existing.setTokenHash("irrelevant-para-este-mock");
        existing.setFamilyId(familyId);
        existing.setUserId(userId);
        existing.setClientId(CLIENT_ID);
        existing.setScope(TokenScope.READ);
        existing.setResource(RESOURCE);
        existing.setExpiresAt(LocalDateTime.now().plusDays(1));
        // revokedAt permanece null: snapshot "ainda ativo" lido por AMBAS as
        // transações concorrentes antes de disputar a rotação.

        when(clientRegistry.require(CLIENT_ID)).thenReturn(new ClientDefinition());
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        // A OUTRA transação já venceu a corrida entre o SELECT acima e este UPDATE.
        when(refreshTokenRepository.markRotated(eq(tokenId), any(LocalDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> tokenService.refresh(rawRefreshToken, CLIENT_ID, RESOURCE))
            .isInstanceOf(OAuthTokenException.class)
            .satisfies(ex -> {
                OAuthTokenException oauthEx = (OAuthTokenException) ex;
                assertThat(oauthEx.getErrorCode()).isEqualTo("invalid_grant");
                assertThat(oauthEx.getMessage()).contains("reuse");
            });

        verify(refreshTokenRepository).revokeFamily(eq(familyId), any(LocalDateTime.class));
        verify(jwtService, never()).issueAccessToken(any(), any(), any(), any());
    }
}
