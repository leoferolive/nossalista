-- V16__add_optimistic_lock_version.sql
-- Adiciona coluna de controle de concorrencia otimista (@Version do JPA) em
-- list_items e lists. Sem essa coluna, duas edicoes concorrentes do mesmo
-- registro podem causar lost update silencioso (o ultimo UPDATE vence sem
-- detectar que o dado mudou entre a leitura e a escrita).

ALTER TABLE list_items ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE lists ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
