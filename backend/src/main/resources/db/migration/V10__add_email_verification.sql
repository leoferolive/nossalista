-- Q2.7: verificação de e-mail no registro
--
-- Adiciona a coluna email_verified em users (default FALSE para contas existentes)
-- e cria a tabela email_verification_tokens (mesmo padrão de password_reset_tokens).
--
-- IMPORTANTE (decisão de produto pendente do dono): contas pré-existentes ficam
-- com email_verified=FALSE. O enforcement estrito de login (bloquear não-verificados)
-- é configurável via app.auth.require-email-verification (default FALSE) para não
-- deslogar usuários existentes. Backfill para TRUE deve ser uma decisão explícita.

ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Usuários autenticados via Google (OAuth) já têm e-mail verificado pelo provedor.
UPDATE users
    SET email_verified = TRUE
    WHERE auth_provider = 'GOOGLE';

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_verification_tokens_token ON email_verification_tokens(token);
CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens(user_id);
