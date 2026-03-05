package br.com.leoferolive.nossalista.list.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Entidade JPA representando tipos de lista pré-definidos.
 *
 * Tipos disponíveis:
 * 1. Compras (slug: compras)
 * 2. Tarefas (slug: tarefas)
 * 3. Wishlist (slug: wishlist)
 * 4. Genérica (slug: generica)
 *
 * Esta entidade é IMUTÁVEL - tipos não são editados após criação.
 */
@Entity(name = "list_types")
@Table(name = "list_types")
@Immutable
public class ListTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Getters apenas (sem setters, @Immutable)

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
