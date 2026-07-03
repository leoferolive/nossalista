-- Dynamic Client Registration (DCR, RFC 7591) para o servidor de autorizacao
-- OAuth 2.1 do MCP (Fase C.1, complementa a Fase C/D-022). O connector
-- "Add connector" do claude.ai tenta DCR automaticamente e falha
-- (registration_endpoint_missing) se o servidor nao anunciar o endpoint —
-- ver docs/DECISIONS.md D-024.

-- Clientes registrados dinamicamente via POST /oauth/register, em paralelo aos
-- clientes ESTATICOS de app.mcp-oauth.clients (que continuam funcionando sem
-- alteracao). client_id gerado pelo servidor (prefixo "dcr_"); sem
-- client_secret: todo cliente DCR aqui e PUBLICO (token_endpoint_auth_method
-- fixado em "none", PKCE obrigatorio no /oauth/authorize, mesma regra dos
-- clientes estaticos claude-ai/claude-code).
--
-- redirect_uris e grant_types sao persistidos como texto delimitado (uma URI
-- por linha / grant types separados por virgula) em vez de uma tabela filha
-- ou coluna JSON — a cardinalidade e pequena (poucas URIs por cliente) e o
-- projeto nao usa colunas JSON em nenhuma outra tabela (achado do design).
--
-- last_used_at NULL identifica um cliente que nunca completou a troca
-- code->token (ou refresh->token) — NUNCA setado em GET /oauth/authorize,
-- publico e sem prova de posse (achado do review, I-1: setar no authorize
-- permitia a um atacante nao-autenticado imunizar clientes-zumbi contra esta
-- varredura). Varrido por McpOAuthCleanupScheduler apos tempo sem uso
-- (app.mcp-oauth.dcr.unused-client-ttl, default 24h) para nao acumular
-- registros de clientes que nunca completaram um fluxo. expires_at fica
-- disponivel para uma TTL rigida futura, mas nao e preenchido nesta fase.
CREATE TABLE mcp_oauth_registered_clients (
    id UUID PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL UNIQUE,
    redirect_uris TEXT NOT NULL,
    scope VARCHAR(50) NOT NULL,
    client_name VARCHAR(200),
    token_endpoint_auth_method VARCHAR(20) NOT NULL,
    grant_types VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_mcp_oauth_registered_clients_client_id ON mcp_oauth_registered_clients(client_id);
CREATE INDEX idx_mcp_oauth_registered_clients_last_used_at ON mcp_oauth_registered_clients(last_used_at);
