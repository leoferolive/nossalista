-- OAuth 2.1 (Authorization Code + PKCE) para clientes MCP externos (claude.ai,
-- Claude Code) se conectarem ao servidor MCP (/mcp) do NossaLista sem depender
-- de Personal Access Tokens copiados manualmente. Ver docs/DECISIONS.md D-021.

-- Passo 1: pedido de autorizacao pendente de consentimento do usuario. Criado em
-- GET /oauth/authorize (antes do login/consentimento), consumido quando o
-- usuario aprova ou nega na tela de consentimento da SPA. TTL curto (10min).
CREATE TABLE mcp_oauth_pending_authorizations (
    id UUID PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    redirect_uri VARCHAR(512) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    state VARCHAR(512),
    code_challenge VARCHAR(255) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_mcp_oauth_pending_authorizations_expires_at ON mcp_oauth_pending_authorizations(expires_at);

-- Passo 2: authorization code emitido apos o usuario aprovar o consentimento.
-- Uso unico (marcado em consumed_at na troca por token, nunca reaproveitado).
-- token_family_id e preenchido na troca bem-sucedida para permitir revogar a
-- familia inteira de refresh tokens caso o MESMO code seja reenviado depois
-- (replay) — ver McpOAuthTokenService.
CREATE TABLE mcp_oauth_codes (
    id UUID PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id VARCHAR(100) NOT NULL,
    redirect_uri VARCHAR(512) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    code_challenge VARCHAR(255) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    token_family_id UUID,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_mcp_oauth_codes_code ON mcp_oauth_codes(code);
CREATE INDEX idx_mcp_oauth_codes_expires_at ON mcp_oauth_codes(expires_at);

-- Refresh tokens (hash SHA-256 apenas, mesmo padrao de personal_access_tokens)
-- com rotacao obrigatoria e deteccao de reuso por familia: family_id identifica
-- a cadeia de rotacoes originada de um mesmo authorization code. Reuso de um
-- refresh ja rotacionado (revoked_at preenchido) revoga toda a familia.
CREATE TABLE mcp_oauth_refresh_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id VARCHAR(100) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    revoked_at TIMESTAMP,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_mcp_oauth_refresh_tokens_family_id ON mcp_oauth_refresh_tokens(family_id);
CREATE INDEX idx_mcp_oauth_refresh_tokens_user_id ON mcp_oauth_refresh_tokens(user_id);
CREATE INDEX idx_mcp_oauth_refresh_tokens_expires_at ON mcp_oauth_refresh_tokens(expires_at);
