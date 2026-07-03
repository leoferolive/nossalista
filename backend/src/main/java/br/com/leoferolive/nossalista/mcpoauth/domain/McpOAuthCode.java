package br.com.leoferolive.nossalista.mcpoauth.domain;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authorization code (RFC 6749 + PKCE) emitido após o usuário aprovar o
 * consentimento de um cliente OAuth do servidor MCP.
 *
 * <p>Uso único: {@link #consumedAt} é preenchido na primeira troca bem-sucedida
 * em {@code POST /oauth/token} — a linha NÃO é removida (ao contrário do
 * {@code OAuthCodeStore} do login Q2.3), porque um reenvio do mesmo code depois
 * de consumido é um sinal de replay e precisa disparar a revogação da família de
 * refresh tokens gerada por ele ({@link #tokenFamilyId}).</p>
 */
@Entity
@Table(name = "mcp_oauth_codes")
public class McpOAuthCode {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "redirect_uri", nullable = false, length = 512)
    private String redirectUri;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private TokenScope scope;

    @Column(name = "code_challenge", nullable = false)
    private String codeChallenge;

    @Column(name = "resource", nullable = false)
    private String resource;

    @Column(name = "token_family_id")
    private UUID tokenFamilyId;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public TokenScope getScope() {
        return scope;
    }

    public void setScope(TokenScope scope) {
        this.scope = scope;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public void setTokenFamilyId(UUID tokenFamilyId) {
        this.tokenFamilyId = tokenFamilyId;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
