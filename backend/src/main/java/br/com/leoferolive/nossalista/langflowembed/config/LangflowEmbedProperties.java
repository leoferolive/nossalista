package br.com.leoferolive.nossalista.langflowembed.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração do embed do Langflow (spike): o NossaLista emite um JWT RS256
 * curto para o usuário logado, e o Langflow valida esse token pela chave pública
 * publicada em {@code /.well-known/jwks.json}.
 *
 * <p>Desligado por padrão. Com {@code enabled=false} nenhuma chave é exigida e os
 * endpoints do embed respondem 404.</p>
 */
@ConfigurationProperties(prefix = "app.langflow-embed")
public class LangflowEmbedProperties {

    /** Liga o embed. Com {@code false}, os endpoints respondem 404 e nenhuma chave é carregada. */
    private boolean enabled = false;

    /** Claim {@code iss} do token e URL base que o Langflow usa para confiar nesta emissão. */
    private String issuer;

    /** Claim {@code aud} do token. */
    private String audience = "langflow";

    /** Validade do token; também vira {@code Max-Age} do cookie. */
    private Duration tokenTtl = Duration.ofMinutes(15);

    /** Chave privada RSA em PEM PKCS#8, sempre via variável de ambiente, nunca em arquivo versionado. */
    private String privateKeyPem;

    /** Valor do header {@code kid}, usado pelo Langflow para escolher a chave no JWKS. */
    private String keyId = "nl-langflow-1";

    /** Nome do cookie que transporta o token até o Langflow. */
    private String cookieName = "nl_lf_token";

    /** Path do cookie; restrito ao caminho do Langflow para não vazar para o SPA. */
    private String cookiePath = "/langflow";

    /** Flag {@code Secure} do cookie. Só desligar em dev sobre HTTP. */
    private boolean cookieSecure = true;

    /** Destino do 302 após gravar o cookie. */
    private String launchRedirect = "/langflow/";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getLaunchRedirect() {
        return launchRedirect;
    }

    public void setLaunchRedirect(String launchRedirect) {
        this.launchRedirect = launchRedirect;
    }
}
