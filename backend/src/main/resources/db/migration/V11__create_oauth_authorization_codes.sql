-- One-time codes do login OAuth2 (Q2.3) persistidos no banco.
--
-- Antes este mapeamento code -> JWT vivia em memória (ConcurrentHashMap) dentro
-- de CADA instância. Isso quebrava o login Google sempre que o code era emitido
-- numa instância (callback do Google) e trocado em outra (XHR /oauth/exchange) —
-- cenário real com >1 réplica/HPA ou quando o pod reinicia entre emitir e trocar:
-- o exchange respondia 400 e o usuário ficava sem JWT (401 em tudo).
--
-- Persistindo no banco compartilhado, qualquer instância valida o code e o fluxo
-- sobrevive a restart/escala. Mesmo padrão de password_reset_tokens / email_verification_tokens.
CREATE TABLE oauth_authorization_codes (
    id UUID PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    jwt VARCHAR(1024) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_oauth_authorization_codes_code ON oauth_authorization_codes(code);
CREATE INDEX idx_oauth_authorization_codes_expires_at ON oauth_authorization_codes(expires_at);
