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
 *
 * <p>Example: {@code UserAuthIdentity identity = new UserAuthIdentity();}</p>
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

    /**
     * Creates an empty identity for JPA or the identity-binding service.
     *
     * <p>Example: {@code UserAuthIdentity identity = new UserAuthIdentity();}</p>
     */
    public UserAuthIdentity() {
    }

    /**
     * Returns the persistent identity id.
     *
     * <p>Example: {@code UUID id = identity.getId();}</p>
     *
     * @return persistent identity id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Assigns the persistent identity id before persistence.
     *
     * <p>Example: {@code identity.setId(UUID.randomUUID());}</p>
     *
     * @param id persistent identity id
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Returns the provider that asserted this identity.
     *
     * <p>Example: {@code String provider = identity.getProvider();}</p>
     *
     * @return provider name, such as {@code GOOGLE}
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Assigns the provider that asserted this identity.
     *
     * <p>Example: {@code identity.setProvider("GOOGLE");}</p>
     *
     * @param provider provider name
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Returns the canonical issuer of the provider assertion.
     *
     * <p>Example: {@code String issuer = identity.getIssuer();}</p>
     *
     * @return canonical provider issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Assigns the canonical issuer of the provider assertion.
     *
     * <p>Example: {@code identity.setIssuer("https://accounts.google.com");}</p>
     *
     * @param issuer canonical provider issuer
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Returns the stable subject supplied by the provider.
     *
     * <p>Example: {@code String subject = identity.getSubject();}</p>
     *
     * @return provider-scoped stable subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Assigns the stable subject supplied by the provider.
     *
     * <p>Example: {@code identity.setSubject("google-subject");}</p>
     *
     * @param subject provider-scoped stable subject
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Returns the local account linked to this identity.
     *
     * <p>Example: {@code UUID userId = identity.getUserId();}</p>
     *
     * @return linked local user id
     */
    public UUID getUserId() {
        return userId;
    }

    /**
     * Links this identity to a local account.
     *
     * <p>Example: {@code identity.setUserId(user.getId());}</p>
     *
     * @param userId linked local user id
     */
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    /**
     * Returns the e-mail most recently asserted by the provider.
     *
     * <p>Example: {@code String email = identity.getProviderEmail();}</p>
     *
     * @return provider-asserted e-mail, when supplied
     */
    public String getProviderEmail() {
        return providerEmail;
    }

    /**
     * Records the e-mail most recently asserted by the provider.
     *
     * <p>Example: {@code identity.setProviderEmail("person@example.com");}</p>
     *
     * @param providerEmail provider-asserted e-mail
     */
    public void setProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    /**
     * Reports whether the provider verified its asserted e-mail.
     *
     * <p>Example: {@code boolean verified = identity.isProviderEmailVerified();}</p>
     *
     * @return whether the provider verified the e-mail
     */
    public boolean isProviderEmailVerified() {
        return providerEmailVerified;
    }

    /**
     * Records whether the provider verified its asserted e-mail.
     *
     * <p>Example: {@code identity.setProviderEmailVerified(true);}</p>
     *
     * @param providerEmailVerified whether the provider verified the e-mail
     */
    public void setProviderEmailVerified(boolean providerEmailVerified) {
        this.providerEmailVerified = providerEmailVerified;
    }

    /**
     * Returns when the identity was first persisted.
     *
     * <p>Example: {@code LocalDateTime createdAt = identity.getCreatedAt();}</p>
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Assigns the identity creation timestamp for a migration fixture.
     *
     * <p>Example: {@code identity.setCreatedAt(LocalDateTime.now());}</p>
     *
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns when the identity was most recently refreshed.
     *
     * <p>Example: {@code LocalDateTime updatedAt = identity.getUpdatedAt();}</p>
     *
     * @return last refresh timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Assigns when the identity was most recently refreshed.
     *
     * <p>Example: {@code identity.setUpdatedAt(LocalDateTime.now());}</p>
     *
     * @param updatedAt last refresh timestamp
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
