-- V4__create_list_items.sql
-- Criação da tabela de itens de lista (Story 3.1 / Epic 3)
-- Suporta campos dinâmicos por tipo: quantity (Compras), due_date (Tarefas), url (Wishlist)
-- UUID gerado via JPA @PrePersist (compatibilidade H2 e PostgreSQL)

CREATE TABLE IF NOT EXISTS list_items (
    id UUID PRIMARY KEY,
    list_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    checked BOOLEAN NOT NULL DEFAULT false,
    quantity INTEGER,
    due_date TIMESTAMP,
    url VARCHAR(500),
    position INTEGER NOT NULL DEFAULT 0,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_list_items_list
        FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_list_items_creator
        FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Índice para busca rápida de itens por lista
CREATE INDEX IF NOT EXISTS idx_list_items_list_id ON list_items(list_id);

-- Índice para ordenação por posição dentro de cada lista
CREATE INDEX IF NOT EXISTS idx_list_items_position ON list_items(list_id, position);
