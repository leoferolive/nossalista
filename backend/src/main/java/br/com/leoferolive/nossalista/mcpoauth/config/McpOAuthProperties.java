package br.com.leoferolive.nossalista.mcpoauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuração estática do servidor de autorização OAuth 2.1 embutido para
 * clientes do servidor MCP (Fase C do roadmap MCP — ver docs/DECISIONS.md D-021).
 *
 * <p>Clientes são registrados ESTATICAMENTE aqui (sem Dynamic Client
 * Registration) — decisão do Passo 0: o claude.ai e o Claude Code aceitam um
 * {@code client_id} (e opcionalmente {@code client_secret}) fixo informado
 * manualmente, então DCR não é pré-requisito de conexão.</p>
 */
@ConfigurationProperties(prefix = "app.mcp-oauth")
public class McpOAuthProperties {

    /**
     * Default do TTL do access token OAuth — CURTO deliberadamente (achado do
     * QA): o access token é um JWT stateless, então revogar a família de
     * refresh tokens (replay/reuso, ver {@code McpOAuthTokenService}) NÃO
     * invalida instantaneamente um access token já emitido — ele continua
     * válido até expirar. Um TTL curto é a defesa primária contra essa janela
     * de exposição; introspecção/blacklist de access tokens fica registrada
     * como follow-up em docs/DECISIONS.md (D-021).
     */
    private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofMinutes(10);

    /** Issuer (RFC 8414) — base URL do servidor de autorização. */
    private String issuer;

    /** Audience canônica (RFC 8707) do servidor MCP — deve bater com o {@code resource} pedido. */
    private String resource;

    /** Chave de assinatura HMAC do access token OAuth — NUNCA a mesma do {@code JWT_SECRET} de sessão. */
    private String signingKey;

    private Duration pendingAuthorizationTtl = Duration.ofMinutes(10);
    private Duration codeTtl = Duration.ofSeconds(60);
    private Duration accessTokenTtl = DEFAULT_ACCESS_TOKEN_TTL;
    private Duration refreshTokenTtl = Duration.ofDays(30);

    private List<ClientDefinition> clients = new ArrayList<>();

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(String signingKey) {
        this.signingKey = signingKey;
    }

    public Duration getPendingAuthorizationTtl() {
        return pendingAuthorizationTtl;
    }

    public void setPendingAuthorizationTtl(Duration pendingAuthorizationTtl) {
        this.pendingAuthorizationTtl = pendingAuthorizationTtl;
    }

    public Duration getCodeTtl() {
        return codeTtl;
    }

    public void setCodeTtl(Duration codeTtl) {
        this.codeTtl = codeTtl;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public List<ClientDefinition> getClients() {
        return clients;
    }

    public void setClients(List<ClientDefinition> clients) {
        this.clients = clients;
    }

    /**
     * Cliente OAuth estático (ex.: {@code claude-ai}, {@code claude-code}).
     *
     * <p>{@link #allowLoopbackRedirect}: quando {@code true}, o
     * {@code redirect_uri} é validado por regra especial em vez de match exato —
     * aceita {@code http://localhost:<qualquer porta>/callback} e
     * {@code http://127.0.0.1:<qualquer porta>/callback} (RFC 8252 §7.3, exceção
     * de loopback para apps nativos). O Claude Code escolhe uma porta aleatória
     * a cada conexão por padrão, então match exato de URI quebraria o fluxo.</p>
     */
    public static class ClientDefinition {

        private String id;
        private String name;
        private List<String> redirectUris = new ArrayList<>();
        private boolean allowLoopbackRedirect;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getRedirectUris() {
            return redirectUris;
        }

        public void setRedirectUris(List<String> redirectUris) {
            this.redirectUris = redirectUris;
        }

        public boolean isAllowLoopbackRedirect() {
            return allowLoopbackRedirect;
        }

        public void setAllowLoopbackRedirect(boolean allowLoopbackRedirect) {
            this.allowLoopbackRedirect = allowLoopbackRedirect;
        }
    }
}
