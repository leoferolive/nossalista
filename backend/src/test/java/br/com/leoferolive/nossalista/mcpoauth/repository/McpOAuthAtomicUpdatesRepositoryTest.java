package br.com.leoferolive.nossalista.mcpoauth.repository;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthCode;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRefreshToken;
import br.com.leoferolive.nossalista.mcpoauth.domain.PendingAuthorization;
import br.com.leoferolive.nossalista.support.AbstractPostgresIT;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, no nível do repositório, que os UPDATEs condicionais introduzidos
 * pelo review (I-1/M-1) são atômicos: a SEGUNDA chamada para o mesmo id
 * afeta 0 linhas mesmo sem nenhuma leitura intermediária decidir isso em
 * Java — a própria query, com {@code WHERE ... IS NULL}, é o compare-and-swap.
 *
 * <p>Isto substitui, para fins de prova de atomicidade, um teste de corrida
 * real com threads: chamando o método condicional duas vezes em sequência já
 * demonstra que a segunda chamada NUNCA reconsome uma linha já consumida —
 * exatamente a garantia que fecha a janela de corrida (duas transações
 * concorrentes, sob READ COMMITTED, disputando a mesma linha seguem esta
 * MESMA semântica: o SGBD serializa as duas execuções do UPDATE e só uma
 * delas vê {@code consumed_at}/{@code revoked_at}/{@code claimed_by_user_id}
 * ainda {@code NULL}).</p>
 *
 * <p>Estende {@link AbstractPostgresIT}: roda contra PostgreSQL real
 * (Testcontainers), não H2 — ver T1 da Onda 2 (honestidade de métrica). É
 * particularmente relevante aqui: a prova de atomicidade depende da
 * semântica real de {@code READ COMMITTED} do Postgres em produção.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class McpOAuthAtomicUpdatesRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private McpOAuthCodeRepository codeRepository;

    @Autowired
    private McpOAuthRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PendingAuthorizationRepository pendingAuthorizationRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = persistUser("owner");
    }

    @Test
    @DisplayName("McpOAuthCodeRepository.markConsumed: só a primeira chamada consome o code")
    void markConsumedIsAtomic() {
        McpOAuthCode code = new McpOAuthCode();
        code.setCode("atomic-code-" + UUID.randomUUID());
        code.setUserId(user.getId());
        code.setClientId("claude-code");
        code.setRedirectUri("http://localhost:8765/callback");
        code.setScope(TokenScope.READ);
        code.setCodeChallenge("challenge");
        code.setResource("http://localhost:8080/mcp");
        code.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        code = codeRepository.saveAndFlush(code);

        UUID firstFamilyId = UUID.randomUUID();
        UUID secondFamilyId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        int firstAttempt = codeRepository.markConsumed(code.getId(), firstFamilyId, now);
        int secondAttempt = codeRepository.markConsumed(code.getId(), secondFamilyId, now);

        assertThat(firstAttempt).as("primeira chamada consome o code").isEqualTo(1);
        assertThat(secondAttempt).as("segunda chamada (replay/corrida) não consome nada").isEqualTo(0);

        // O family_id persistido é o da chamada VENCEDORA (a primeira), nunca o
        // da segunda tentativa — evidencia que o UPDATE condicional realmente
        // rejeitou a segunda escrita, em vez de sobrescrever silenciosamente.
        // Projeção ESCALAR (não a entidade): `code` já está no cache de 1º
        // nível desta transação, então um findByCode comum devolveria a mesma
        // instância cacheada (tokenFamilyId nunca mutado em Java).
        UUID persistedFamilyId = codeRepository.findTokenFamilyIdByCode(code.getCode()).orElseThrow();
        assertThat(persistedFamilyId).isEqualTo(firstFamilyId);
    }

    @Test
    @DisplayName("McpOAuthRefreshTokenRepository.markRotated: só a primeira chamada rotaciona o token")
    void markRotatedIsAtomic() {
        McpOAuthRefreshToken refreshToken = new McpOAuthRefreshToken();
        refreshToken.setTokenHash("hash-" + UUID.randomUUID());
        refreshToken.setFamilyId(UUID.randomUUID());
        refreshToken.setUserId(user.getId());
        refreshToken.setClientId("claude-code");
        refreshToken.setScope(TokenScope.READ);
        refreshToken.setResource("http://localhost:8080/mcp");
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(30));
        refreshToken = refreshTokenRepository.saveAndFlush(refreshToken);

        LocalDateTime now = LocalDateTime.now();
        int firstAttempt = refreshTokenRepository.markRotated(refreshToken.getId(), now);
        int secondAttempt = refreshTokenRepository.markRotated(refreshToken.getId(), now);

        assertThat(firstAttempt).as("primeira rotação revoga o token atual").isEqualTo(1);
        assertThat(secondAttempt).as("segunda rotação (reuso/corrida) não revoga nada de novo").isEqualTo(0);
    }

    @Test
    @DisplayName("PendingAuthorizationRepository.claimIfUnclaimed: só a primeira conta reivindica o pedido")
    void claimIfUnclaimedIsAtomic() {
        PendingAuthorization pending = new PendingAuthorization();
        pending.setClientId("claude-code");
        pending.setRedirectUri("http://localhost:8765/callback");
        pending.setScope(TokenScope.READ);
        pending.setCodeChallenge("challenge");
        pending.setResource("http://localhost:8080/mcp");
        pending.setNonce("nonce-" + UUID.randomUUID());
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        pending = pendingAuthorizationRepository.saveAndFlush(pending);

        // claimed_by_user_id tem FK para users(id): precisa de contas reais,
        // não UUIDs aleatórios.
        User firstUser = persistUser("first");
        User secondUser = persistUser("second");

        int firstClaim = pendingAuthorizationRepository.claimIfUnclaimed(pending.getId(), firstUser.getId());
        int secondClaim = pendingAuthorizationRepository.claimIfUnclaimed(pending.getId(), secondUser.getId());

        assertThat(firstClaim).as("primeira conta reivindica o pedido").isEqualTo(1);
        assertThat(secondClaim).as("segunda conta (sequestro/corrida) não rouba a reivindicação").isEqualTo(0);

        // Leitura ESCALAR (não a entidade cacheada) confirma quem realmente ficou
        // com a reivindicação no banco: a primeira conta, nunca a segunda.
        UUID claimedBy = pendingAuthorizationRepository.findClaimedByUserId(pending.getId()).orElseThrow();
        assertThat(claimedBy).isEqualTo(firstUser.getId());
    }

    private User persistUser(String label) {
        String suffix = label + "-" + UUID.randomUUID().toString().substring(0, 8);
        User newUser = new User();
        newUser.setUsername("atomic" + suffix);
        newUser.setEmail("atomic" + suffix + "@example.com");
        newUser.setPassword("hashed");
        newUser.setAuthProvider(AuthProvider.EMAIL);
        newUser.setRole(Role.USER);
        return userRepository.saveAndFlush(newUser);
    }
}
