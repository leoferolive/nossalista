package br.com.leoferolive.nossalista.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stable external-provider identity linked to a local user account.
 *
 * <p>The provider subject, rather than an e-mail address, is the identity key.
 * Authentication behavior and provider binding are implemented by the auth
 * service; this entity only maps the V19 schema foundation.</p>
 */
@Entity
@Table(name = "user_auth_identities")
public class UserAuthIdentity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "issuer", nullable = false, length = 255)
    private String issuer;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider_email")
    private String providerEmail;

    @Column(name = "provider_email_verified", nullable = false)
    private boolean providerEmailVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UserAuthIdentity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public void setProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    public boolean isProviderEmailVerified() {
        return providerEmailVerified;
    }

    public void setProviderEmailVerified(boolean providerEmailVerified) {
        this.providerEmailVerified = providerEmailVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
