package br.com.leoferolive.nossalista.langflowembed.service;

import br.com.leoferolive.nossalista.langflowembed.LangflowEmbedTestKeys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LangflowEmbedPemParser")
class LangflowEmbedPemParserTest {

    private static final KeyPair KEYS_2048 = LangflowEmbedTestKeys.generate(2048);

    @Test
    @DisplayName("deve ler PEM PKCS#8 e derivar a pública do mesmo par")
    void parsesPkcs8AndDerivesPublicKey() {
        LangflowEmbedRsaKeys keys = LangflowEmbedPemParser.parse(LangflowEmbedTestKeys.pkcs8Pem(KEYS_2048.getPrivate()));

        assertThat(keys.privateKey().getModulus()).isEqualTo(((RSAPrivateKey) KEYS_2048.getPrivate()).getModulus());
        assertThat(keys.publicKey().getModulus()).isEqualTo(keys.privateKey().getModulus());
        assertThat(keys.publicKey().getPublicExponent())
            .isEqualTo(((RSAPublicKey) KEYS_2048.getPublic()).getPublicExponent());
    }

    @Test
    @DisplayName("deve aceitar \\n escapado literalmente, como em variáveis de ambiente de k8s e CI")
    void acceptsEscapedNewlines() {
        String escaped = LangflowEmbedTestKeys.pkcs8Pem(KEYS_2048.getPrivate()).replace("\n", "\\n");

        LangflowEmbedRsaKeys keys = LangflowEmbedPemParser.parse(escaped);

        assertThat(keys.publicKey().getModulus()).isEqualTo(((RSAPrivateKey) KEYS_2048.getPrivate()).getModulus());
    }

    @Test
    @DisplayName("deve rejeitar PKCS#1 com orientação de conversão, sem ecoar a chave")
    void rejectsPkcs1WithoutEchoingKey() {
        String pkcs1 = "-----BEGIN " + "RSA PRIVATE KEY-----\nFAKEBODYFAKEBODY\n-----END " + "RSA PRIVATE KEY-----\n";

        assertThatThrownBy(() -> LangflowEmbedPemParser.parse(pkcs1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PKCS#8")
            .hasMessageNotContaining("FAKEBODYFAKEBODY");
    }

    @Test
    @DisplayName("deve rejeitar chave menor que 2048 bits")
    void rejectsKeyBelow2048Bits() {
        KeyPair small = LangflowEmbedTestKeys.generate(1024);
        String pem = LangflowEmbedTestKeys.pkcs8Pem(small.getPrivate());

        assertThatThrownBy(() -> LangflowEmbedPemParser.parse(pem))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("1024 bits")
            .hasMessageContaining("2048");
    }

    @Test
    @DisplayName("deve rejeitar texto sem cabeçalho PKCS#8")
    void rejectsMissingHeaders() {
        assertThatThrownBy(() -> LangflowEmbedPemParser.parse("nao-e-um-pem"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BEGIN PRIVATE KEY")
            .hasMessageNotContaining("nao-e-um-pem");
    }

    @Test
    @DisplayName("deve rejeitar corpo base64 que não é uma chave, sem ecoar o corpo")
    void rejectsGarbageBodyWithoutEchoingIt() {
        String garbage = "-----BEGIN " + "PRIVATE KEY-----\nAAAA\n-----END " + "PRIVATE KEY-----\n";

        assertThatThrownBy(() -> LangflowEmbedPemParser.parse(garbage))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PKCS#8")
            .hasMessageNotContaining("AAAA");
    }
}
