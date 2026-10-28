package br.com.leoferolive.nossalista.langflowembed.service;

import br.com.leoferolive.nossalista.langflowembed.LangflowEmbedTestKeys;
import br.com.leoferolive.nossalista.langflowembed.config.LangflowEmbedProperties;
import br.com.leoferolive.nossalista.langflowembed.dto.LangflowJwksResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LangflowEmbedKeyService")
class LangflowEmbedKeyServiceTest {

    private static final KeyPair KEYS = LangflowEmbedTestKeys.generate(2048);
    private static final String ISSUER = "https://nossalista-test.example.com.br";

    private LangflowEmbedProperties enabledProperties() {
        LangflowEmbedProperties properties = new LangflowEmbedProperties();
        properties.setEnabled(true);
        properties.setIssuer(ISSUER);
        properties.setPrivateKeyPem(LangflowEmbedTestKeys.pkcs8Pem(KEYS.getPrivate()));
        return properties;
    }

    private LangflowEmbedKeyService started(LangflowEmbedProperties properties) {
        LangflowEmbedKeyService service = new LangflowEmbedKeyService(properties);
        service.loadKeysWhenEnabled();
        return service;
    }

    @Nested
    @DisplayName("embed desligado")
    class Disabled {

        @Test
        @DisplayName("não exige chave nem issuer na inicialização")
        void doesNotRequireKeyOrIssuer() {
            LangflowEmbedKeyService service = new LangflowEmbedKeyService(new LangflowEmbedProperties());

            assertThatCode(service::loadKeysWhenEnabled).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("recusa usar a chave privada quando nunca foi carregada")
        void refusesToSignWhenNotLoaded() {
            LangflowEmbedKeyService service = new LangflowEmbedKeyService(new LangflowEmbedProperties());
            service.loadKeysWhenEnabled();

            assertThatThrownBy(service::privateKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.langflow-embed.enabled=true");
        }
    }

    @Nested
    @DisplayName("embed ligado")
    class Enabled {

        @Test
        @DisplayName("falha na inicialização sem issuer, citando a variável")
        void failsWithoutIssuer() {
            LangflowEmbedProperties properties = enabledProperties();
            properties.setIssuer(" ");

            assertThatThrownBy(() -> new LangflowEmbedKeyService(properties).loadKeysWhenEnabled())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LANGFLOW_EMBED_ISSUER");
        }

        @Test
        @DisplayName("falha na inicialização sem chave privada, citando a variável")
        void failsWithoutPrivateKey() {
            LangflowEmbedProperties properties = enabledProperties();
            properties.setPrivateKeyPem(null);

            assertThatThrownBy(() -> new LangflowEmbedKeyService(properties).loadKeysWhenEnabled())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LANGFLOW_EMBED_RSA_PRIVATE_KEY");
        }

        @Test
        @DisplayName("expõe a chave privada carregada e o kid configurado")
        void exposesLoadedKeyAndKeyId() {
            LangflowEmbedKeyService service = started(enabledProperties());

            assertThat(service.privateKey().getModulus())
                .isEqualTo(((RSAPrivateKey) KEYS.getPrivate()).getModulus());
            assertThat(service.keyId()).isEqualTo("nl-langflow-1");
        }

        @Test
        @DisplayName("JWKS tem uma chave RSA de assinatura, sem zero à esquerda no módulo")
        void jwksHasSingleRs256SigningKey() {
            LangflowJwksResponse jwks = started(enabledProperties()).jwks();

            assertThat(jwks.keys()).hasSize(1);
            var jwk = jwks.keys().get(0);
            assertThat(jwk.kty()).isEqualTo("RSA");
            assertThat(jwk.use()).isEqualTo("sig");
            assertThat(jwk.alg()).isEqualTo("RS256");
            assertThat(jwk.kid()).isEqualTo("nl-langflow-1");
            assertThat(Base64.getUrlDecoder().decode(jwk.n())).hasSize(256);
            assertThat(Base64.getUrlDecoder().decode(jwk.n())[0]).isNotZero();
        }
    }
}
