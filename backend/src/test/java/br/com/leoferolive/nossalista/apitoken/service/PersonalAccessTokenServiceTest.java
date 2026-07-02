package br.com.leoferolive.nossalista.apitoken.service;

import br.com.leoferolive.nossalista.apitoken.domain.PersonalAccessToken;
import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.dto.PersonalAccessTokenCreatedResponse;
import br.com.leoferolive.nossalista.apitoken.dto.PersonalAccessTokenMapper;
import br.com.leoferolive.nossalista.apitoken.exception.PersonalAccessTokenLimitExceededException;
import br.com.leoferolive.nossalista.apitoken.exception.PersonalAccessTokenNotFoundException;
import br.com.leoferolive.nossalista.apitoken.repository.PersonalAccessTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalAccessTokenService")
class PersonalAccessTokenServiceTest {

    @Mock
    private PersonalAccessTokenRepository repository;

    private PersonalAccessTokenService service;

    @BeforeEach
    void setUp() {
        service = new PersonalAccessTokenService(repository, new PersonalAccessTokenMapper());
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("create gera token em claro com prefixo nlmcp_ e persiste apenas o hash SHA-256")
    void createGeneratesTokenAndPersistsOnlyHash() {
        UUID userId = UUID.randomUUID();
        when(repository.countByUserIdAndRevokedAtIsNull(userId)).thenReturn(0L);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePersonalAccessTokenRequest request =
            new CreatePersonalAccessTokenRequest("Claude Desktop", TokenScope.READ, null);

        PersonalAccessTokenCreatedResponse response = service.create(userId, request);

        assertThat(response.token()).startsWith("nlmcp_");
        assertThat(response.prefix()).startsWith("nlmcp_");
        assertThat(response.scope()).isEqualTo(TokenScope.READ);
        assertThat(response.expiresAt()).isNull();

        ArgumentCaptor<PersonalAccessToken> captor = ArgumentCaptor.forClass(PersonalAccessToken.class);
        verify(repository).save(captor.capture());
        PersonalAccessToken saved = captor.getValue();

        assertThat(saved.getTokenHash()).isEqualTo(sha256Hex(response.token()));
        assertThat(saved.getTokenHash()).isNotEqualTo(response.token());
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getName()).isEqualTo("Claude Desktop");
    }

    @Test
    @DisplayName("create gera tokens únicos entre chamadas")
    void createGeneratesUniqueTokens() {
        UUID userId = UUID.randomUUID();
        when(repository.countByUserIdAndRevokedAtIsNull(userId)).thenReturn(0L);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePersonalAccessTokenRequest request =
            new CreatePersonalAccessTokenRequest("Token", TokenScope.READ_WRITE, null);

        String token1 = service.create(userId, request).token();
        String token2 = service.create(userId, request).token();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("create calcula expiresAt a partir de expiresInDays quando informado")
    void createComputesExpiresAtFromExpiresInDays() {
        UUID userId = UUID.randomUUID();
        when(repository.countByUserIdAndRevokedAtIsNull(userId)).thenReturn(0L);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePersonalAccessTokenRequest request =
            new CreatePersonalAccessTokenRequest("Token 30d", TokenScope.READ, 30);

        LocalDateTime before = LocalDateTime.now().plusDays(30).minusSeconds(5);
        PersonalAccessTokenCreatedResponse response = service.create(userId, request);
        LocalDateTime after = LocalDateTime.now().plusDays(30).plusSeconds(5);

        assertThat(response.expiresAt()).isNotNull();
        assertThat(response.expiresAt()).isAfter(before).isBefore(after);
    }

    @Test
    @DisplayName("create lança exceção quando limite de tokens ativos é atingido")
    void createThrowsWhenLimitReached() {
        UUID userId = UUID.randomUUID();
        when(repository.countByUserIdAndRevokedAtIsNull(userId)).thenReturn(10L);

        CreatePersonalAccessTokenRequest request =
            new CreatePersonalAccessTokenRequest("Token 11", TokenScope.READ, null);

        assertThatThrownBy(() -> service.create(userId, request))
            .isInstanceOf(PersonalAccessTokenLimitExceededException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("list retorna metadados sem hash, delegando ao repositório")
    void listReturnsMetadataFromRepository() {
        UUID userId = UUID.randomUUID();
        PersonalAccessToken token = new PersonalAccessToken();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setName("Token A");
        token.setPrefix("nlmcp_abc123");
        token.setScope(TokenScope.READ);
        when(repository.findAllByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId)).thenReturn(List.of(token));

        var responses = service.list(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("Token A");
        assertThat(responses.get(0).prefix()).isEqualTo("nlmcp_abc123");
    }

    @Test
    @DisplayName("revoke marca revokedAt quando token está ativo")
    void revokeMarksRevokedAtWhenActive() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        PersonalAccessToken token = new PersonalAccessToken();
        token.setId(tokenId);
        token.setUserId(userId);
        when(repository.findByIdAndUserId(tokenId, userId)).thenReturn(Optional.of(token));

        service.revoke(tokenId, userId);

        assertThat(token.getRevokedAt()).isNotNull();
        verify(repository).save(token);
    }

    @Test
    @DisplayName("revoke é idempotente: token já revogado não é salvo novamente")
    void revokeIsIdempotent() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        PersonalAccessToken token = new PersonalAccessToken();
        token.setId(tokenId);
        token.setUserId(userId);
        token.setRevokedAt(LocalDateTime.now().minusDays(1));
        when(repository.findByIdAndUserId(tokenId, userId)).thenReturn(Optional.of(token));

        service.revoke(tokenId, userId);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("revoke lança exceção quando token não existe ou pertence a outro usuário")
    void revokeThrowsWhenNotFoundOrOtherUser() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        when(repository.findByIdAndUserId(tokenId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(tokenId, userId))
            .isInstanceOf(PersonalAccessTokenNotFoundException.class);
    }

    @Test
    @DisplayName("authenticate retorna vazio para valor nulo ou sem o prefixo nlmcp_")
    void authenticateReturnsEmptyForInvalidPrefix() {
        assertThat(service.authenticate(null)).isEmpty();
        assertThat(service.authenticate("not-a-pat-token")).isEmpty();
    }

    @Test
    @DisplayName("authenticate retorna vazio quando hash não é encontrado")
    void authenticateReturnsEmptyWhenNotFound() {
        String rawToken = "nlmcp_" + "a".repeat(64);
        when(repository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.empty());

        assertThat(service.authenticate(rawToken)).isEmpty();
    }

    @Test
    @DisplayName("authenticate retorna vazio para token revogado")
    void authenticateReturnsEmptyForRevokedToken() {
        String rawToken = "nlmcp_" + "b".repeat(64);
        PersonalAccessToken token = new PersonalAccessToken();
        token.setRevokedAt(LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(token));

        assertThat(service.authenticate(rawToken)).isEmpty();
    }

    @Test
    @DisplayName("authenticate retorna vazio para token expirado")
    void authenticateReturnsEmptyForExpiredToken() {
        String rawToken = "nlmcp_" + "c".repeat(64);
        PersonalAccessToken token = new PersonalAccessToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(token));

        assertThat(service.authenticate(rawToken)).isEmpty();
    }

    @Test
    @DisplayName("authenticate retorna o token e grava last_used_at quando nunca usado")
    void authenticateUpdatesLastUsedAtWhenNull() {
        String rawToken = "nlmcp_" + "d".repeat(64);
        PersonalAccessToken token = new PersonalAccessToken();
        token.setLastUsedAt(null);
        when(repository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(token));

        Optional<PersonalAccessToken> result = service.authenticate(rawToken);

        assertThat(result).isPresent();
        assertThat(token.getLastUsedAt()).isNotNull();
        verify(repository).save(token);
    }

    @Test
    @DisplayName("authenticate não atualiza last_used_at dentro da janela de throttle de 60s")
    void authenticateThrottlesLastUsedUpdate() {
        String rawToken = "nlmcp_" + "e".repeat(64);
        LocalDateTime recentUse = LocalDateTime.now().minusSeconds(10);
        PersonalAccessToken token = new PersonalAccessToken();
        token.setLastUsedAt(recentUse);
        when(repository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(token));

        service.authenticate(rawToken);

        assertThat(token.getLastUsedAt()).isEqualTo(recentUse);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("authenticate atualiza last_used_at após a janela de throttle de 60s")
    void authenticateUpdatesLastUsedAfterThrottleWindow() {
        String rawToken = "nlmcp_" + "f".repeat(64);
        LocalDateTime oldUse = LocalDateTime.now().minusSeconds(120);
        PersonalAccessToken token = new PersonalAccessToken();
        token.setLastUsedAt(oldUse);
        when(repository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(token));

        service.authenticate(rawToken);

        assertThat(token.getLastUsedAt()).isAfter(oldUse);
        verify(repository, times(1)).save(token);
    }
}
