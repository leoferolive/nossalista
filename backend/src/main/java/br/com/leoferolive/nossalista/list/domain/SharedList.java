package br.com.leoferolive.nossalista.list.domain;

import br.com.leoferolive.nossalista.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import br.com.leoferolive.nossalista.list.util.ListTypeMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA representando uma lista compartilhada.
 *
 * Relacionamentos:
 * - ManyToOne com User (owner): Dono da lista
 * - ManyToOne com ListTypeEntity: Tipo da lista (Compras, Tarefas, etc)
 *
 * Campos dinâmicos por tipo serão implementados em list_items (Story 3.1).
 */
@Entity(name = "lists")
@Table(name = "lists")
public class SharedList {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Nome da lista é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "type_id", nullable = false)
    private Integer typeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ListTypeEntity typeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "invite_code", unique = true, length = 20)
    private String inviteCode;

    @Column(name = "invite_expires_at")
    private LocalDateTime inviteExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Controle de concorrência otimista (lock otimista do JPA). Incrementado a cada
     * UPDATE bem-sucedido; uma escrita com versão desatualizada lança
     * {@link org.springframework.orm.ObjectOptimisticLockingFailureException} em vez
     * de sobrescrever silenciosamente a alteração de outro membro (lost update).
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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
     * Retorna o tipo da lista como enum ListType.
     *
     * @return o ListType correspondente ao typeId
     */
    public ListType getType() {
        String slug = ListTypeMapper.resolveSlug(typeEntity, typeId);
        return ListType.fromSlug(slug);
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public ListTypeEntity getTypeEntity() {
        return typeEntity;
    }

    public void setTypeEntity(ListTypeEntity typeEntity) {
        this.typeEntity = typeEntity;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public LocalDateTime getInviteExpiresAt() {
        return inviteExpiresAt;
    }

    public void setInviteExpiresAt(LocalDateTime inviteExpiresAt) {
        this.inviteExpiresAt = inviteExpiresAt;
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

    public Long getVersion() {
        return version;
    }
}
