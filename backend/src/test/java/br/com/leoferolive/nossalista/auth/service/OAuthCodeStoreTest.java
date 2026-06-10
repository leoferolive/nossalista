package br.com.leoferolive.nossalista.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuthCodeStore (one-time code — Q2.3)")
class OAuthCodeStoreTest {

    private static final String JWT = "eyJhbGciOiJIUzI1NiJ9.payload.signature";

    @Test
    @DisplayName("issue gera codes opacos, únicos e URL-safe")
    void issueGeneratesUniqueUrlSafeCodes() {
        OAuthCodeStore store = new OAuthCodeStore();

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
        OAuthCodeStore store = new OAuthCodeStore();
        String code = store.issue(JWT);

        assertThat(store.consume(code)).contains(JWT);
    }

    @Test
    @DisplayName("consume é single-use: um code só pode ser trocado uma vez")
    void consumeIsSingleUse() {
        OAuthCodeStore store = new OAuthCodeStore();
        String code = store.issue(JWT);

        assertThat(store.consume(code)).contains(JWT);
        // Segunda troca falha (já consumido)
        assertThat(store.consume(code)).isEmpty();
    }

    @Test
    @DisplayName("consume retorna vazio para code inexistente, nulo ou em branco")
    void consumeReturnsEmptyForUnknownCode() {
        OAuthCodeStore store = new OAuthCodeStore();

        assertThat(store.consume("inexistente")).isEmpty();
        assertThat(store.consume(null)).isEmpty();
        assertThat(store.consume("   ")).isEmpty();
    }

    @Test
    @DisplayName("consume retorna vazio para code expirado")
    void consumeReturnsEmptyForExpiredCode() {
        // TTL negativo: code já nasce expirado
        OAuthCodeStore store = new OAuthCodeStore(Duration.ofMillis(-1));
        String code = store.issue(JWT);

        Optional<String> result = store.consume(code);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("evictExpired remove codes expirados e mantém os válidos")
    void evictExpiredRemovesExpiredCodes() {
        OAuthCodeStore expiredStore = new OAuthCodeStore(Duration.ofMillis(-1));
        expiredStore.issue(JWT);
        assertThat(expiredStore.size()).isEqualTo(1);

        expiredStore.evictExpired();
        assertThat(expiredStore.size()).isZero();

        OAuthCodeStore validStore = new OAuthCodeStore(Duration.ofMinutes(1));
        validStore.issue(JWT);
        validStore.evictExpired();
        assertThat(validStore.size()).isEqualTo(1);
    }
}
