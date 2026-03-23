-- V8__allow_null_created_by_on_list_items.sql
-- LGPD compliance: allow created_by to be NULL so items survive when creator deletes their account.
-- Changes the FK constraint to ON DELETE SET NULL.

ALTER TABLE list_items ALTER COLUMN created_by DROP NOT NULL;

ALTER TABLE list_items DROP CONSTRAINT fk_list_items_creator;

ALTER TABLE list_items
    ADD CONSTRAINT fk_list_items_creator
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;
