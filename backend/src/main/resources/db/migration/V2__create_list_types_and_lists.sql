-- V2__create_list_types_and_lists.sql
-- Cria tabelas list_types (lookup) e lists (entidade principal)

-- Tabela list_types (lookup table - tipos pre-definidos de lista)
CREATE TABLE list_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Insere 4 tipos pre-definidos
INSERT INTO list_types (id, name, slug) VALUES
    (1, 'Compras', 'compras'),
    (2, 'Tarefas', 'tarefas'),
    (3, 'Wishlist', 'wishlist'),
    (4, 'Generica', 'generica');

-- Tabela lists (entidade principal)
-- UUID gerado via JPA @PrePersist (compatibilidade H2 e PostgreSQL)
CREATE TABLE lists (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type_id INTEGER NOT NULL,
    owner_id UUID NOT NULL,
    invite_code VARCHAR(20) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_lists_type FOREIGN KEY (type_id) REFERENCES list_types(id),
    CONSTRAINT fk_lists_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indices para performance
CREATE INDEX idx_lists_owner_id ON lists(owner_id);
CREATE INDEX idx_lists_invite_code ON lists(invite_code);
