package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.OAuthAuthorizationCode;
import br.com.leoferolive.nossalista.auth.repository.OAuthAuthorizationCodeRepository;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do OAuthCodeStore (one-time code — Q2.3) agora PERSISTIDO no banco.
 *
 * <p>O store passou de in-memory (por instância) para banco compartilhado: era a
 * causa do login Google quebrar em produção (code emitido numa instância e
 * trocado em outra → exchange 400). Por isso o teste é de integração — exercita
 * o round-trip real save/find/delete contra o H2 (MODE=PostgreSQL).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("OAuthCodeStore (one-time code persistido — Q2.3)")
class OAuthCodeStoreTest {

    private static final String JWT = "eyJhbGciOiJIUzI1NiJ9.payload.signature";

    @Autowired
    private OAuthAuthorizationCodeRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthCodeStore oauthCodeStore;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    private OAuthCodeStore store(Duration ttl) {
        return new OAuthCodeStore(repository, jwtService, userService, ttl);
    }

    private OAuthCodeStore store() {
        return store(Duration.ofMinutes(1));
    }

    private UUID persistUser() {
        User user = new User();
        String suffix = UUID.randomUUID().toString();
        user.setUsername("oauth-code-" + suffix);
        user.setEmail(suffix + "@example.com");
        user.setPassword("hash");
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(Role.USER);
        return userRepository.saveAndFlush(user).getId();
    }

    @Test
    @DisplayName("issue gera codes opacos, únicos e URL-safe")
    void issueGeneratesUniqueUrlSafeCodes() {
        OAuthCodeStore store = store();

        String code1 = store.issue(JWT);
        String code2 = store.issue(JWT);

        assertThat(code1).isNotBlank();
        assertThat(code2).isNotBlank();
        assertThat(code1).isNotEqualTo(code2);
        // Base64 URL-safe sem padding: não vaza o JWT e não tem '+', '/' nem '='
        assertThat(code1).doesNotContain(JWT);
        assertThat(code1).doesNotContain("+").doesNotContain("/").doesNotContain("=");
    }

    @Test
    @DisplayName("issue para usuário dual-write code legado e JWT com a versão de sessão atual")
    void issueForUserDualWritesLegacyCodeAndCurrentSessionJwt() {
        UUID userId = persistUser();

        String code = oauthCodeStore.issue(userId);

        OAuthAuthorizationCode entity = repository.findByCodeHash(sha256Hex(code)).orElseThrow();
        assertThat(entity.getCodeHash()).isEqualTo(sha256Hex(code));
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getCode()).isEqualTo(code);
        assertThat(entity.getJwt()).isNotBlank();
        assertThat(jwtService.extractUserId(entity.getJwt())).isEqualTo(userId);
        assertThat(jwtService.extractSessionVersion(entity.getJwt())).isZero();
        assertThat(code).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    @DisplayName("claim de usuário marca consumed_at e não pode ser repetido")
    void claimForUserIsAtomicAndSingleUse() {
        OAuthCodeStore store = store();
        UUID userId = persistUser();
        String code = store.issue(userId);

        Optional<OAuthCodeStore.OAuthCodeClaim> first = store.consumeForExchange(code);
        Optional<OAuthCodeStore.OAuthCodeClaim> second = store.consumeForExchange(code);

        assertThat(first).get().extracting(OAuthCodeStore.OAuthCodeClaim::userId).isEqualTo(userId);
        assertThat(second).isEmpty();
        assertThat(repository.findByCodeHash(sha256Hex(code)).orElseThrow().getConsumedAt())
            .isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("consume devolve o JWT para code válido")
    void consumeReturnsJwtForValidCode() {
        OAuthCodeStore store = store();
        String code = store.issue(JWT);

        assertThat(store.consume(code)).contains(JWT);
    }

    @Test
    @DisplayName("consume é single-use: um code só pode ser trocado uma vez")
    void consumeIsSingleUse() {
        OAuthCodeStore store = store();
        String code = store.issue(JWT);

        assertThat(store.consume(code)).contains(JWT);
        // Segunda troca falha (já consumido / removido)
        assertThat(store.consume(code)).isEmpty();
    }

    @Test
    @DisplayName("consume retorna vazio para code inexistente, nulo ou em branco")
    void consumeReturnsEmptyForUnknownCode() {
        OAuthCodeStore store = store();

        assertThat(store.consume("inexistente")).isEmpty();
        assertThat(store.consume(null)).isEmpty();
        assertThat(store.consume("   ")).isEmpty();
    }

    @Test
    @DisplayName("consume retorna vazio para code expirado")
    void consumeReturnsEmptyForExpiredCode() {
        // TTL negativo: code já nasce expirado
        OAuthCodeStore store = store(Duration.ofMillis(-1));
        String code = store.issue(JWT);

        Optional<String> result = store.consume(code);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("evictExpired remove codes expirados e mantém os válidos")
    void evictExpiredRemovesExpiredCodes() {
        OAuthCodeStore expiredStore = store(Duration.ofMillis(-1));
        expiredStore.issue(JWT);
        assertThat(repository.count()).isEqualTo(1);

        expiredStore.evictExpired();
        assertThat(repository.count()).isZero();

        OAuthCodeStore validStore = store(Duration.ofMinutes(1));
        validStore.issue(JWT);
        validStore.evictExpired();
        assertThat(repository.count()).isEqualTo(1);
    }

    private String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception exception) {
            throw new AssertionError("SHA-256 indisponível no teste", exception);
        }
    }
}
