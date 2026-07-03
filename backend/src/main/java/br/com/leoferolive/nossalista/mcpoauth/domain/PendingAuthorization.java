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
 * Pedido de autorização OAuth pendente de consentimento do usuário.
 *
 * <p>Criado por {@code GET /oauth/authorize} antes de qualquer login/consentimento
 * — guarda os parâmetros do pedido (client, redirect_uri, scope, PKCE challenge,
 * resource) até o usuário aprovar ou negar na tela de consentimento da SPA. TTL
 * curto (ver {@code app.mcp-oauth.pending-authorization-ttl}); varrido pelo
 * {@code McpOAuthCleanupScheduler}.</p>
 *
 * <p><b>Sequestro de consentimento cross-user (achado do QA):</b> sem vínculo a
 * um browser/usuário, um atacante não-logado podia gerar um {@code request_id}
 * com o PRÓPRIO {@code code_challenge}, enviar o link por phishing à vítima, e a
 * aprovação da vítima emitiria um code com o {@code userId} da vítima mas o
 * challenge do atacante. Duas defesas: {@link #nonce} (cookie HttpOnly/Secure/
 * SameSite=Lax devolvido só ao browser que chamou {@code /oauth/authorize},
 * exigido em approve/deny) e {@link #claimedByUserId} (trava o pedido ao
 * primeiro usuário autenticado que o visualizar).</p>
 */
@Entity
@Table(name = "mcp_oauth_pending_authorizations")
public class PendingAuthorization {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "redirect_uri", nullable = false, length = 512)
    private String redirectUri;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private TokenScope scope;

    @Column(name = "state", length = 512)
    private String state;

    @Column(name = "code_challenge", nullable = false)
    private String codeChallenge;

    @Column(name = "resource", nullable = false)
    private String resource;

    @Column(name = "nonce", nullable = false)
    private String nonce;

    @Column(name = "claimed_by_user_id")
    private UUID claimedByUserId;

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public UUID getClaimedByUserId() {
        return claimedByUserId;
    }

    public void setClaimedByUserId(UUID claimedByUserId) {
        this.claimedByUserId = claimedByUserId;
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
