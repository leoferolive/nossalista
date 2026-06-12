package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.repository.OAuthAuthorizationCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

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

    private OAuthCodeStore store(Duration ttl) {
        return new OAuthCodeStore(repository, ttl);
    }

    private OAuthCodeStore store() {
        return store(Duration.ofMinutes(1));
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
}
