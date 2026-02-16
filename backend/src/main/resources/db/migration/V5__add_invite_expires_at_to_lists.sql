-- V5__add_invite_expires_at_to_lists.sql
-- Adiciona coluna de expiração para códigos de convite (Story 4.1 / Epic 4)
-- Permite definir validade de 24h para links de convite

ALTER TABLE lists ADD COLUMN invite_expires_at TIMESTAMP;
