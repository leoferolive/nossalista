package br.com.leoferolive.nossalista.listitem.domain;

import br.com.leoferolive.nossalista.list.domain.SharedList;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA representando um item de lista.
 *
 * Campos dinâmicos por tipo:
 * - quantity: Usado em listas do tipo Compras
 * - dueDate: Usado em listas do tipo Tarefas
 * - url: Usado em listas do tipo Wishlist
 *
 * Relacionamentos:
 * - ManyToOne com List: Lista à qual o item pertence
 * - ManyToOne com User: Usuário que criou o item
 */
@Entity(name = "list_items")
@Table(name = "list_items")
public class ListItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Nome do item é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private boolean checked = false;

    @Column
    private Integer quantity;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Size(max = 500, message = "URL deve ter no máximo 500 caracteres")
    @Column(length = 500)
    private String url;

    @Column(nullable = false)
    private Integer position = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private SharedList list;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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

    // Constructors
    public ListItem() {
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

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public SharedList getList() {
        return list;
    }

    public void setList(SharedList list) {
        this.list = list;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
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
