-- Personal Access Tokens (PAT): autenticacao de clientes MCP/API externos por usuario.
-- Apenas o hash SHA-256 do token e persistido; o valor em claro e retornado uma
-- unica vez na criacao (ver docs/DECISIONS.md).

CREATE TABLE personal_access_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    prefix VARCHAR(16) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_personal_access_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_personal_access_tokens_user_id ON personal_access_tokens(user_id);
