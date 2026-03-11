package br.com.leoferolive.nossalista.push;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VapidConfig Tests")
class VapidConfigTest {

    @Test
    @DisplayName("isConfigured retorna false quando publicKey é nula")
    void isConfigured_false_whenPublicKeyIsNull() {
        VapidConfig config = new VapidConfig();
        config.setPrivateKey("privateKey");
        assertThat(config.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("isConfigured retorna false quando publicKey é vazia")
    void isConfigured_false_whenPublicKeyIsBlank() {
        VapidConfig config = new VapidConfig();
        config.setPublicKey("   ");
        config.setPrivateKey("privateKey");
        assertThat(config.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("isConfigured retorna false quando privateKey é nula")
    void isConfigured_false_whenPrivateKeyIsNull() {
        VapidConfig config = new VapidConfig();
        config.setPublicKey("publicKey");
        assertThat(config.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("isConfigured retorna false quando privateKey é vazia")
    void isConfigured_false_whenPrivateKeyIsBlank() {
        VapidConfig config = new VapidConfig();
        config.setPublicKey("publicKey");
        config.setPrivateKey("  ");
        assertThat(config.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("isConfigured retorna true quando ambas as chaves estão definidas")
    void isConfigured_true_whenBothKeysAreSet() {
        VapidConfig config = new VapidConfig();
        config.setPublicKey("publicKey");
        config.setPrivateKey("privateKey");
        config.setSubject("mailto:test@test.com");
        assertThat(config.isConfigured()).isTrue();
    }
}
