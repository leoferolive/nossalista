-- Authentication hardening foundation.
-- Keep the legacy OAuth columns during the rolling deployment. New handoff rows
-- dual-write code_hash/user_id/consumed_at and code/jwt until pre-V19 application
-- instances have been absent for longer than the 60-second handoff TTL.

ALTER TABLE users
    ADD COLUMN session_version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE user_auth_identities (
    id UUID PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_email VARCHAR(255),
    provider_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_auth_identities_provider_issuer_subject
        UNIQUE (provider, issuer, subject),
    CONSTRAINT uq_user_auth_identities_user_issuer
        UNIQUE (user_id, issuer)
);

CREATE INDEX idx_user_auth_identities_user_id
    ON user_auth_identities(user_id);

ALTER TABLE oauth_authorization_codes
    ALTER COLUMN code DROP NOT NULL;

ALTER TABLE oauth_authorization_codes
    ALTER COLUMN jwt DROP NOT NULL;

ALTER TABLE oauth_authorization_codes
    ADD COLUMN code_hash VARCHAR(64);

ALTER TABLE oauth_authorization_codes
    ADD COLUMN user_id UUID REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE oauth_authorization_codes
    ADD COLUMN consumed_at TIMESTAMP;

CREATE UNIQUE INDEX uq_oauth_authorization_codes_code_hash
    ON oauth_authorization_codes(code_hash);

CREATE INDEX idx_oauth_authorization_codes_user_id
    ON oauth_authorization_codes(user_id);

CREATE INDEX idx_oauth_authorization_codes_consumed_at
    ON oauth_authorization_codes(consumed_at);
