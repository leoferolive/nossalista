package br.com.leoferolive.nossalista.langflowembed.service;

import br.com.leoferolive.nossalista.langflowembed.config.LangflowEmbedProperties;
import br.com.leoferolive.nossalista.langflowembed.dto.LangflowJwksMapper;
import br.com.leoferolive.nossalista.langflowembed.dto.LangflowJwksResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;

/**
 * Guarda a chave de assinatura do embed e expõe a pública no JWKS.
 *
 * <p>Carrega a chave na inicialização (fail-fast) somente com {@code enabled=true}.
 * Com o embed desligado, nada é lido e nenhuma variável é exigida.</p>
 */
@Service
public class LangflowEmbedKeyService {

    private final LangflowEmbedProperties properties;
    private LangflowEmbedRsaKeys keys;

    public LangflowEmbedKeyService(LangflowEmbedProperties properties) {
        this.properties = properties;
    }

    /**
     * Valida issuer e chave na inicialização, mesmo padrão de {@code JwtService.validateSecret}.
     *
     * @throws IllegalStateException se o embed estiver ligado sem issuer ou sem chave válida
     */
    @PostConstruct
    void loadKeysWhenEnabled() {
        if (!properties.isEnabled()) {
            return;
        }
        requireIssuer();
        keys = LangflowEmbedPemParser.parse(requirePrivateKeyPem());
    }

    /**
     * @return chave privada RSA usada para assinar o token do Langflow
     * @throws IllegalStateException se o embed não estiver ligado
     */
    public RSAPrivateKey privateKey() {
        return requireKeys().privateKey();
    }

    /**
     * @return identificador da chave, copiado para o header {@code kid} do token
     */
    public String keyId() {
        return properties.getKeyId();
    }

    /**
     * @return JWKS com a chave pública, pronto para {@code GET /.well-known/jwks.json}
     * @throws IllegalStateException se o embed não estiver ligado
     */
    public LangflowJwksResponse jwks() {
        return LangflowJwksMapper.toJwks(keyId(), requireKeys().publicKey());
    }

    private LangflowEmbedRsaKeys requireKeys() {
        if (keys == null) {
            throw new IllegalStateException(
                "chave do Langflow não carregada: defina app.langflow-embed.enabled=true");
        }
        return keys;
    }

    private void requireIssuer() {
        if (properties.getIssuer() == null || properties.getIssuer().isBlank()) {
            throw new IllegalStateException(
                "app.langflow-embed.issuer ausente: defina LANGFLOW_EMBED_ISSUER (URL pública do NossaLista) "
                    + "quando o embed do Langflow estiver ligado");
        }
    }

    private String requirePrivateKeyPem() {
        String pem = properties.getPrivateKeyPem();
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException(
                "LANGFLOW_EMBED_RSA_PRIVATE_KEY ausente: defina a chave privada RSA PKCS#8 "
                    + "quando o embed do Langflow estiver ligado");
        }
        return pem;
    }
}
