-- Cria tabela users para testes
-- Baseado em V1__create_users_table.sql

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    name VARCHAR(100),
    avatar_url VARCHAR(500),
    auth_provider VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Inserir um usuário de teste para o GET /me
INSERT INTO users (id, username, email, password, name, avatar_url, auth_provider, role, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-0001',
    'testuser',
    'test@example.com',
    'hashedpassword',
    'Test User',
    'https://example.com/avatar.jpg',
    'EMAIL',
    'USER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
