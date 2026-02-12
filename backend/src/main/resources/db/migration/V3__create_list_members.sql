-- V3__create_list_members.sql
-- Criação da tabela de membros de lista (Story 2.3 / Epic 2)
-- Permite que usuários sejam membros de listas criadas por outros usuários
-- UUID gerado via JPA @PrePersist (compatibilidade H2 e PostgreSQL)

CREATE TABLE IF NOT EXISTS list_members (
    id UUID PRIMARY KEY,
    list_id UUID NOT NULL REFERENCES lists(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(list_id, user_id)
);

-- Índice para busca rápida de membros por lista
CREATE INDEX IF NOT EXISTS idx_list_members_list_id ON list_members(list_id);

-- Índice para busca rápida de listas por usuário (usado em getAllListsForUser)
CREATE INDEX IF NOT EXISTS idx_list_members_user_id ON list_members(user_id);
