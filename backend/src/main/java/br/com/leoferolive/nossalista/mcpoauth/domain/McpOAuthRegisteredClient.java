package br.com.leoferolive.nossalista.mcpoauth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Cliente OAuth registrado dinamicamente via {@code POST /oauth/register}
 * (Dynamic Client Registration, RFC 7591 — ver docs/DECISIONS.md D-024).
 *
 * <p>Complementa (não substitui) os clientes ESTÁTICOS de
 * {@code app.mcp-oauth.clients}: {@code McpOAuthClientRegistry} consulta os
 * dois — estáticos têm precedência por {@code id}. Todo cliente aqui é
 * PÚBLICO (sem {@code client_secret}, PKCE obrigatório em
 * {@code /oauth/authorize}) — não há coluna de segredo nesta tabela.</p>
 *
 * <p>{@link #redirectUris}/{@link #grantTypes} são persistidos como texto
 * delimitado (uma URI por linha / grant types separados por vírgula) — ver
 * migration V14.</p>
 */
@Entity
@Table(name = "mcp_oauth_registered_clients")
public class McpOAuthRegisteredClient {

    private static final String REDIRECT_URI_DELIMITER = "\n";
    private static final String GRANT_TYPE_DELIMITER = ",";

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "client_id", nullable = false, unique = true, length = 64)
    private String clientId;

    @Column(name = "redirect_uris", nullable = false)
    private String redirectUrisRaw;

    @Column(name = "scope", nullable = false, length = 50)
    private String scope;

    @Column(name = "client_name", length = 200)
    private String clientName;

    @Column(name = "token_endpoint_auth_method", nullable = false, length = 20)
    private String tokenEndpointAuthMethod;

    @Column(name = "grant_types", nullable = false, length = 200)
    private String grantTypesRaw;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
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

    public List<String> getRedirectUris() {
        return splitOrEmpty(redirectUrisRaw, REDIRECT_URI_DELIMITER);
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUrisRaw = String.join(REDIRECT_URI_DELIMITER, redirectUris);
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    public List<String> getGrantTypes() {
        return splitOrEmpty(grantTypesRaw, GRANT_TYPE_DELIMITER);
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypesRaw = String.join(GRANT_TYPE_DELIMITER, grantTypes);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    private static List<String> splitOrEmpty(String raw, String delimiter) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.asList(raw.split(delimiter));
    }
}
